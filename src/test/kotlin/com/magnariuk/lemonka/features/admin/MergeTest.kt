package com.magnariuk.lemonka.features.admin

import com.magnariuk.lemonka.auth.AuthResult
import com.magnariuk.lemonka.auth.AuthService
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.module
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.*

class MergeTest {
    @Test
    fun `merge accounts - projects, actors, characters, role`() = testApplication {
        val file = Files.createTempFile("lemonka-merge", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath) }
        application { module() }
        startApplication()
        val main = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        fun inv() = kotlinx.coroutines.runBlocking { InviteRepository.create(main.user.id).code }
        val old = AuthService.register("magnar_old", "12345678", inv()) as AuthResult.Success
        val alt = AuthService.register("magnar_alt", "12345678", inv()) as AuthResult.Success
        val keep = AuthService.register("keepme", "12345678", inv()) as AuthResult.Success

        // Same project name on both sides, same actor name on both sides
        ProjectRepository.create("Frieren", keep.user.id)
        val oldFrieren = ProjectRepository.create("Frieren", old.user.id)
        val oldOther = ProjectRepository.create("Dandadan", old.user.id)
        ActorRepository.add("Олена", keep.user.id)
        ActorRepository.add("Олена", old.user.id)
        ActorRepository.add("Максим", alt.user.id)
        val keepOlena = ActorRepository.byName("Олена", keep.user.id)!!
        val oldOlena = ActorRepository.byName("Олена", old.user.id)!!
        AssignmentRepository.add(oldOlena.id, oldFrieren.id)
        CharacterRepository.add("Fern", oldFrieren.id)
        CharacterRepository.assign(CharacterRepository.byName("Fern", oldFrieren.id)!!.id, oldOlena.id)
        UserRepository.setRole(alt.user.id, Role.Admin)

        val c = createClient { followRedirects = false }
        fun form(vararg p: Pair<String, String>) = FormDataContent(Parameters.build { p.forEach { append(it.first, it.second) } })
        val auth: HttpRequestBuilder.() -> Unit = { cookie("token", main.token.token); header("HX-Request", "true") }

        // Can't merge your own account away
        assertContains(c.post("/admin/users/merge") { auth(); setBody(form("target" to "${keep.user.id}", "source" to "${main.user.id}")) }.bodyAsText(), "поточний акаунт")
        assertContains(c.post("/admin/users/merge") { auth(); setBody(form("target" to "${keep.user.id}")) }.bodyAsText(), "оберіть акаунти")

        val r = c.post("/admin/users/merge") {
            auth(); setBody(form("target" to "${keep.user.id}", "source" to "${old.user.id}", "source" to "${alt.user.id}"))
        }.bodyAsText()
        assertContains(r, "Об'єднано") // toast

        // Sources gone, their logins dead
        assertNull(UserRepository.byId(old.user.id)); assertNull(UserRepository.byId(alt.user.id))
        assertSame(AuthResult.UserNotFound, AuthService.login("magnar_old", "12345678"))

        // Projects moved, clash renamed
        assertEquals(setOf("Frieren", "Frieren (magnar_old)", "Dandadan"), ProjectRepository.forUser(keep.user.id).map { it.name }.toSet())
        assertEquals(keep.user.id, ProjectRepository.byId(oldOther.id)!!.user)

        // Actors with the same name joined; character and assignment point at the kept actor
        assertEquals(listOf("Максим", "Олена"), ActorRepository.forUser(keep.user.id).map { it.name }.sorted())
        assertNull(ActorRepository.byId(oldOlena.id))
        assertEquals(keepOlena.id, CharacterRepository.byName("Fern", oldFrieren.id)!!.actor)
        assertNotNull(AssignmentRepository.find(oldFrieren.id, keepOlena.id))

        // An admin was merged in → kept account becomes admin
        assertEquals(Role.Admin, UserRepository.byId(keep.user.id)!!.role)
        assertIs<AuthResult.Success>(AuthService.login("keepme", "12345678"))
    }
}

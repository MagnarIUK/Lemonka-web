package com.magnariuk.lemonka.features.profile

import com.magnariuk.lemonka.auth.AuthResult
import com.magnariuk.lemonka.auth.AuthService
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.module
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.*

class ProfileTest {
    private class Ctx(val client: HttpClient, val admin: AuthResult.Success, val other: AuthResult.Success)

    private fun profileTest(block: suspend Ctx.() -> Unit) = testApplication {
        val file = Files.createTempFile("lemonka-profile", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath) }
        application { module() }
        startApplication()
        val admin = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        val invite = InviteRepository.create(admin.user.id)
        val other = AuthService.register("other", "12345678", invite.code) as AuthResult.Success
        Ctx(createClient { followRedirects = false }, admin, other).block()
    }

    private fun HttpRequestBuilder.auth(s: AuthResult.Success) { cookie("token", s.token.token); header("HX-Request", "true") }
    private fun form(vararg p: Pair<String, String>) = FormDataContent(Parameters.build { p.forEach { append(it.first, it.second) } })

    @Test
    fun `profile lists own projects and actors only`() = profileTest {
        ProjectRepository.create("Mine", admin.user.id)
        ProjectRepository.create("Theirs", other.user.id)
        ActorRepository.add("Ivan", admin.user.id)
        val page = client.get("/profile") { auth(admin) }.bodyAsText()
        assertContains(page, "Mine"); assertContains(page, "Ivan")
        assertFalse(page.contains("Theirs"))
    }

    @Test
    fun `cannot delete or view someone else's project, actor or character`() = profileTest {
        val theirs = ProjectRepository.create("Theirs", other.user.id)
        CharacterRepository.add("Hero", theirs.id)
        val hero = CharacterRepository.byName("Hero", theirs.id)!!
        ActorRepository.add("TheirActor", other.user.id)
        val theirActor = ActorRepository.byName("TheirActor", other.user.id)!!

        for (r in listOf(
            client.delete("/profile/projects/${theirs.id}") { auth(admin) },
            client.get("/profile/projects/${theirs.id}") { auth(admin) },
            client.delete("/profile/characters/${hero.id}") { auth(admin) },
            client.delete("/profile/actors/${theirActor.id}") { auth(admin) },
        )) {
            assertEquals("none", r.headers["HX-Reswap"]); assertContains(r.bodyAsText(), "Не знайдено")
        }
        assertNotNull(ProjectRepository.byId(theirs.id))
        assertNotNull(CharacterRepository.byId(hero.id))
        assertNotNull(ActorRepository.byId(theirActor.id))
    }

    @Test
    fun `delete own project cascades, add and delete actor`() = profileTest {
        val p = ProjectRepository.create("Show", admin.user.id)
        CharacterRepository.addMissing(listOf("A", "B"), p.id)
        assertContains(client.delete("/profile/projects/${p.id}") { auth(admin) }.bodyAsText(), "видалено")
        assertNull(ProjectRepository.byId(p.id))
        assertTrue(CharacterRepository.forProject(p.id).isEmpty())

        assertContains(client.post("/profile/actors") { auth(admin); setBody(form("name" to "Olena")) }.bodyAsText(), "додано")
        assertContains(client.post("/profile/actors") { auth(admin); setBody(form("name" to "Olena")) }.bodyAsText(), "такий актор уже є")
        val olena = ActorRepository.byName("Olena", admin.user.id)!!
        client.delete("/profile/actors/${olena.id}") { auth(admin) }
        assertNull(ActorRepository.byId(olena.id))
    }

    @Test
    fun `change password`() = profileTest {
        assertContains(client.post("/profile/password") { auth(admin); setBody(form("current" to "wrong", "new" to "newpass123", "repeat" to "newpass123")) }.bodyAsText(), "невірний пароль")
        assertContains(client.post("/profile/password") { auth(admin); setBody(form("current" to "12345678", "new" to "newpass123", "repeat" to "different")) }.bodyAsText(), "паролі не збігаються")
        assertContains(client.post("/profile/password") { auth(admin); setBody(form("current" to "12345678", "new" to "newpass123", "repeat" to "newpass123")) }.bodyAsText(), "Пароль змінено")
        assertIs<AuthResult.Success>(AuthService.login("magnar", "newpass123"))
    }

    @Test
    fun `last admin cannot delete account, normal user can`() = profileTest {
        assertContains(client.post("/profile/delete") { auth(admin); setBody(form("password" to "12345678")) }.bodyAsText(), "єдиний адміністратор")
        assertNotNull(UserRepository.byId(admin.user.id))

        ProjectRepository.create("Gone", other.user.id)
        val r = client.post("/profile/delete") { auth(other); setBody(form("password" to "12345678")) }
        assertEquals("/login", r.headers["HX-Redirect"])
        assertNull(UserRepository.byId(other.user.id))
        assertTrue(ProjectRepository.forUser(other.user.id).isEmpty())
    }

    @Test
    fun `logout other sessions keeps the current one`() = profileTest {
        val second = TokenRepository.create(admin.user.id)
        client.post("/profile/sessions/logout-others") { auth(admin) }
        assertNull(TokenRepository.findValid(second.token))
        assertNotNull(TokenRepository.findValid(admin.token.token))
    }
}

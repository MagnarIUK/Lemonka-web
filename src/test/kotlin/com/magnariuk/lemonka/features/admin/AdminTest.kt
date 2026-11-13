package com.magnariuk.lemonka.features.admin

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

class AdminTest {
    private class Ctx(val client: HttpClient, val admin: AuthResult.Success, val user: AuthResult.Success)

    private fun adminTest(block: suspend Ctx.() -> Unit) = testApplication {
        val file = Files.createTempFile("lemonka-admin", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath, "lemonka.publicUrl" to "https://tools.example.com/") }
        application { module() }
        startApplication()
        val admin = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        val user = AuthService.register("normal", "12345678", InviteRepository.create(admin.user.id).code) as AuthResult.Success
        Ctx(createClient { followRedirects = false }, admin, user).block()
    }

    private fun HttpRequestBuilder.auth(s: AuthResult.Success) { cookie("token", s.token.token); header("HX-Request", "true") }
    private fun form(vararg p: Pair<String, String>) = FormDataContent(Parameters.build { p.forEach { append(it.first, it.second) } })

    @Test
    fun `normal users cannot open or use the admin panel`() = adminTest {
        for (path in listOf("/admin/invites", "/admin/users", "/admin/separators")) {
            assertEquals(HttpStatusCode.Forbidden, client.get(path) { cookie("token", user.token.token) }.status)
        }
        assertEquals(HttpStatusCode.Forbidden, client.post("/admin/invites") { auth(user); setBody(form("maxUses" to "5")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/admin/users/${user.user.id}/role") { auth(user); setBody(form("role" to "admin")) }.status)
        assertEquals(Role.User, UserRepository.byId(user.user.id)!!.role)
    }

    @Test
    fun `admin menu entry only for admins`() = adminTest {
        assertContains(client.get("/") { cookie("token", admin.token.token) }.bodyAsText(), "/admin")
        assertFalse(client.get("/") { cookie("token", user.token.token) }.bodyAsText().contains("href=\"/admin\""))
    }

    @Test
    fun `create, revoke and delete invites, with public link`() = adminTest {
        val html = client.post("/admin/invites") { auth(admin); setBody(form("maxUses" to "3", "validDays" to "0", "note" to "для Олени")) }.bodyAsText()
        val invite = InviteRepository.all().first()
        assertEquals(3, invite.maxUses); assertNull(invite.expiresAt); assertEquals("для Олени", invite.note)
        assertContains(html, "https://tools.example.com/login?mode=register&amp;invite=${invite.code}")

        client.post("/admin/invites/${invite.id}/revoke") { auth(admin) }
        assertTrue(InviteRepository.byId(invite.id)!!.revoked)
        client.delete("/admin/invites/${invite.id}") { auth(admin) }
        assertNull(InviteRepository.byId(invite.id))
    }

    @Test
    fun `roles - promote, and last admin is protected`() = adminTest {
        assertContains(client.post("/admin/users/${admin.user.id}/role") { auth(admin); setBody(form("role" to "user")) }.bodyAsText(), "останнього адміністратора")
        assertEquals(Role.Admin, UserRepository.byId(admin.user.id)!!.role)

        client.post("/admin/users/${user.user.id}/role") { auth(admin); setBody(form("role" to "admin")) }
        assertEquals(Role.Admin, UserRepository.byId(user.user.id)!!.role)
        // Now there are two admins, so demoting one is fine
        client.post("/admin/users/${user.user.id}/role") { auth(admin); setBody(form("role" to "user")) }
        assertEquals(Role.User, UserRepository.byId(user.user.id)!!.role)
    }

    @Test
    fun `reset password logs the user out and the temp password works`() = adminTest {
        val html = client.post("/admin/users/${user.user.id}/reset-password") { auth(admin) }.bodyAsText()
        val temp = Regex("""<div class="code code-big">([^<]+)</div>""").find(html)!!.groupValues[1]
        assertNull(TokenRepository.findValid(user.token.token))
        assertIs<AuthResult.Success>(AuthService.login("normal", temp))
    }

    @Test
    fun `delete users - not yourself, others yes`() = adminTest {
        assertContains(client.delete("/admin/users/${admin.user.id}") { auth(admin) }.bodyAsText(), "у профілі")
        ProjectRepository.create("P", user.user.id)
        client.delete("/admin/users/${user.user.id}") { auth(admin) }
        assertNull(UserRepository.byId(user.user.id))
        assertTrue(ProjectRepository.forUser(user.user.id).isEmpty())
    }

    @Test
    fun `separators - add, reject duplicate, delete`() = adminTest {
        client.post("/admin/separators") { auth(admin); setBody(form("separator" to "&")) }
        assertContains(SeparatorRepository.all(), "&")
        assertContains(client.post("/admin/separators") { auth(admin); setBody(form("separator" to "&")) }.bodyAsText(), "такий роздільник уже є")
        val amp = SeparatorRepository.list().first { it.separator == "&" }
        client.delete("/admin/separators/${amp.id}") { auth(admin) }
        assertFalse(SeparatorRepository.all().contains("&"))
    }
}

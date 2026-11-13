package com.magnariuk.lemonka.auth

import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.module
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class InviteTest {
    @BeforeTest
    fun setUp() {
        val file = Files.createTempFile("lemonka-invite", ".db").toFile().apply { deleteOnExit() }
        DatabaseFactory.init(file.absolutePath)
    }

    @Test
    fun `first account is admin, others need a valid invite`() = runBlocking {
        assertFalse(AuthService.inviteRequired())
        val first = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        assertEquals(Role.Admin, first.user.role)

        assertTrue(AuthService.inviteRequired())
        assertSame(AuthResult.InvalidInvite, AuthService.register("second", "12345678", null))
        assertSame(AuthResult.InvalidInvite, AuthService.register("second", "12345678", "NOPE-NOPE"))

        val invite = InviteRepository.create(first.user.id, maxUses = 1)
        assertTrue(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}").matches(invite.code))

        // Typed sloppily: lower case, no dash
        val sloppy = invite.code.lowercase().replace("-", "")
        val second = AuthService.register("second", "12345678", sloppy) as AuthResult.Success
        assertEquals(Role.User, second.user.role)
        assertEquals(invite.id, second.user.invitedBy)

        // Used up
        assertSame(AuthResult.InvalidInvite, AuthService.register("third", "12345678", invite.code))
    }

    @Test
    fun `multi-use, revoked and expired invites`() = runBlocking {
        val admin = (AuthService.register("magnar", "12345678", null) as AuthResult.Success).user

        val multi = InviteRepository.create(admin.id, maxUses = 2)
        assertIs<AuthResult.Success>(AuthService.register("aaaa", "12345678", multi.code))
        assertIs<AuthResult.Success>(AuthService.register("bbbb", "12345678", multi.code))
        assertSame(AuthResult.InvalidInvite, AuthService.register("cccc", "12345678", multi.code))

        val revoked = InviteRepository.create(admin.id)
        InviteRepository.revoke(revoked.id)
        assertSame(AuthResult.InvalidInvite, AuthService.register("dddd", "12345678", revoked.code))

        val expired = InviteRepository.create(admin.id, validFor = (-1).seconds)
        assertSame(AuthResult.InvalidInvite, AuthService.register("eeee", "12345678", expired.code))

        // A failed registration must not burn an invite or create the user
        assertNull(UserRepository.byUsername("dddd"))
    }

    @Test
    fun `admin routes are blocked for normal users`() = testApplication {
        val file = Files.createTempFile("lemonka-admin", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath) }
        application {
            module()
            routing { authenticate(AUTH) { requireRole(Role.Admin) { get("/test-admin") { call.respondText("secret") } } } }
        }
        startApplication()
        val admin = (AuthService.register("magnar", "12345678", null) as AuthResult.Success)
        val invite = InviteRepository.create(admin.user.id)
        val user = AuthService.register("normal", "12345678", invite.code) as AuthResult.Success

        val c = createClient { followRedirects = false }
        assertEquals("secret", c.get("/test-admin") { cookie("token", admin.token.token) }.bodyAsText())
        assertEquals(HttpStatusCode.Forbidden, c.get("/test-admin") { cookie("token", user.token.token) }.status)
    }

    @Test
    fun `register form shows invite field and reports bad code`() = testApplication {
        val file = Files.createTempFile("lemonka-form", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath) }
        application { module() }
        val c = createClient { followRedirects = false }

        // First user: no invite field, admin note shown
        val firstForm = c.get("/login?mode=register").bodyAsText()
        assertFalse(firstForm.contains("name=\"invite\""))
        assertContains(firstForm, "права адміністратора")
        c.post("/auth/register") { setBody(FormDataContent(Parameters.build { append("username", "magnar"); append("password", "12345678") })) }

        // Afterwards: invite field present and prefilled from link
        assertContains(c.get("/login?mode=register&invite=ABCD-EFGH").bodyAsText(), "value=\"ABCD-EFGH\"")
        val bad = c.post("/auth/register") {
            header("HX-Request", "true")
            setBody(FormDataContent(Parameters.build { append("username", "other"); append("password", "12345678"); append("invite", "ZZZZ-ZZZZ") }))
        }.bodyAsText()
        assertContains(bad, "код запрошення недійсний")
    }
}

package com.magnariuk.lemonka.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.magnariuk.lemonka.data.db.UserRepository
import com.magnariuk.lemonka.module
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.*

class AuthTest {
    private fun authTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val db = Files.createTempFile("lemonka-auth", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to db.absolutePath) }
        application { module() }
        block()
    }

    private fun form(user: String, pass: String, next: String? = null) = FormDataContent(Parameters.build {
        append("username", user); append("password", pass); next?.let { append("next", it) }
    })

    @Test
    fun `protected page redirects to login`() = authTest {
        val c = createClient { followRedirects = false }
        val r = c.get("/")
        assertEquals(HttpStatusCode.Found, r.status)
        assertEquals("/login?next=%2F", r.headers[HttpHeaders.Location])
    }

    @Test
    fun `register, use cookie, logout`() = authTest {
        val c = createClient { followRedirects = false; install(io.ktor.client.plugins.cookies.HttpCookies) }

        assertContains(c.post("/auth/register") { setBody(form("ab", "12345678")) }.bodyAsText(), "ім'я користувача занадто коротке")
        assertContains(c.post("/auth/register") { setBody(form("magnar", "123")) }.bodyAsText(), "пароль занадто короткий")

        val reg = c.post("/auth/register") { setBody(form("magnar", "12345678", next = "/tools")) }
        assertEquals("/tools", reg.headers[HttpHeaders.Location])
        assertContains(c.get("/").bodyAsText(), "Привіт, magnar!")

        assertContains(c.post("/auth/register") { setBody(form("magnar", "12345678")) }.bodyAsText(), "користувач з таким ім'ям вже існує")

        c.post("/auth/logout")
        assertEquals(HttpStatusCode.Found, c.get("/").status)
    }

    @Test
    fun `login errors, htmx redirect, open redirect blocked`() = authTest {
        val c = createClient { followRedirects = false }
        c.post("/auth/register") { setBody(form("magnar", "12345678")) }

        assertContains(c.post("/auth/login") { setBody(form("nobody", "12345678")) }.bodyAsText(), "користувача не знайдено")
        assertContains(c.post("/auth/login") { setBody(form("magnar", "wrongpass")) }.bodyAsText(), "невірний пароль")

        val r = c.post("/auth/login") { header("HX-Request", "true"); setBody(form("magnar", "12345678", next = "//evil.com")) }
        assertEquals(HttpStatusCode.NoContent, r.status)
        assertEquals("/", r.headers["HX-Redirect"])
    }

    @Test
    fun `old spring bcrypt hashes still log in`() = authTest {
        // Spring's BCryptPasswordEncoder produces $2a$10$ hashes
        startApplication()
        val springHash = BCrypt.with(BCrypt.Version.VERSION_2A).hashToString(10, "oldpassword".toCharArray())
        UserRepository.create("olduser", springHash)
        val c = createClient { followRedirects = false }
        val r = c.post("/auth/login") { setBody(form("olduser", "oldpassword")) }
        assertEquals("/", r.headers[HttpHeaders.Location])
    }
}

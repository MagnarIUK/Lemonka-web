package com.magnariuk.lemonka.features.sheet

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
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.*

class SheetTest {
    private fun png(w: Int, h: Int, alpha: Boolean = false): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(w, h, if (alpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    @Test
    fun `images are scaled down, alpha kept, junk rejected`() {
        val big = Images.process(png(2000, 1000), "big.png") as Images.Result.Ok
        assertEquals("image/jpeg", big.contentType)
        val read = ImageIO.read(big.bytes.inputStream())
        assertEquals(800, read.width); assertEquals(400, read.height)

        val transparent = Images.process(png(100, 100, alpha = true), "t.png") as Images.Result.Ok
        assertEquals("image/png", transparent.contentType)

        assertIs<Images.Result.Error>(Images.process("hello".toByteArray(), "x.png"))
    }

    private class Ctx(val client: HttpClient, val me: AuthResult.Success, val other: AuthResult.Success, val project: Project)

    private fun sheetTest(block: suspend Ctx.() -> Unit) = testApplication {
        val file = Files.createTempFile("lemonka-sheet", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath, "lemonka.publicUrl" to "https://tools.example.com") }
        application { module() }
        startApplication()
        val me = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        val other = AuthService.register("other", "12345678", InviteRepository.create(me.user.id).code) as AuthResult.Success
        val p = ProjectRepository.create("Frieren", me.user.id)
        CharacterRepository.addMissing(listOf("Fern", "Stark"), p.id)
        ActorRepository.add("Олена", me.user.id)
        val olena = ActorRepository.byName("Олена", me.user.id)!!
        AssignmentRepository.add(olena.id, p.id)
        CharacterRepository.assign(CharacterRepository.byName("Fern", p.id)!!.id, olena.id)
        Ctx(createClient { followRedirects = false }, me, other, p).block()
    }

    private fun HttpRequestBuilder.auth(s: AuthResult.Success) { cookie("token", s.token.token); header("HX-Request", "true") }

    private suspend fun Ctx.uploadImage(who: AuthResult.Success, characterId: Int, bytes: ByteArray) =
        client.post("/tools/sub/${project.id}/images/$characterId") {
            auth(who)
            setBody(MultiPartFormDataContent(formData {
                append("image", bytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"pic.png\""); append(HttpHeaders.ContentType, "image/png")
                })
            }))
        }

    @Test
    fun `upload image, show it on the sheet`() = sheetTest {
        val fern = CharacterRepository.byName("Fern", project.id)!!
        assertContains(uploadImage(me, fern.id, png(300, 300)).bodyAsText(), "збережено")
        val img = client.get("/tools/sub/${project.id}/images/${fern.id}") { auth(me) }
        assertEquals(ContentType.Image.JPEG, img.contentType())

        val sheet = client.get("/tools/sub/${project.id}/sheet") { cookie("token", me.token.token) }.bodyAsText()
        assertContains(sheet, "Олена"); assertContains(sheet, "Fern"); assertContains(sheet, "Без актора")
        assertContains(sheet, "/tools/sub/${project.id}/images/${fern.id}?v=")
    }

    @Test
    fun `others cannot upload to or view my pictures in the app`() = sheetTest {
        val fern = CharacterRepository.byName("Fern", project.id)!!
        assertContains(uploadImage(other, fern.id, png(10, 10)).bodyAsText(), "Не знайдено")
        assertNull(CharacterImageRepository.get(fern.id))
        assertEquals(HttpStatusCode.NotFound, client.get("/tools/sub/${project.id}/sheet") { cookie("token", other.token.token) }.status)
    }

    @Test
    fun `public link - off by default, on, works without login, regenerate, off`() = sheetTest {
        val fern = CharacterRepository.byName("Fern", project.id)!!
        uploadImage(me, fern.id, png(50, 50))

        client.post("/tools/sub/${project.id}/share?action=on") { auth(me) }
        val token = ProjectRepository.byId(project.id)!!.shareToken!!
        val anon = client.get("/share/$token").bodyAsText()        // no cookie at all
        assertContains(anon, "Frieren"); assertContains(anon, "Fern")
        assertFalse(anon.contains("Публічне посилання"), "no share controls on the public page")
        val olena = ActorRepository.byName("Олена", me.user.id)!!
        assertContains(client.get("/share/$token?actor=${olena.id}").bodyAsText(), "https://tools.example.com/share/$token?actor=${olena.id}")
        assertEquals(HttpStatusCode.OK, client.get("/share/$token/img/${fern.id}").status)

        // A character from another project can't be fetched through this token
        val otherProject = ProjectRepository.create("Secret", other.user.id)
        CharacterRepository.add("Hidden", otherProject.id)
        val hidden = CharacterRepository.byName("Hidden", otherProject.id)!!
        CharacterImageRepository.save(hidden.id, "image/png", png(5, 5))
        assertEquals(HttpStatusCode.NotFound, client.get("/share/$token/img/${hidden.id}").status)

        client.post("/tools/sub/${project.id}/share?action=regen") { auth(me) }
        assertEquals(HttpStatusCode.NotFound, client.get("/share/$token").status)
        client.post("/tools/sub/${project.id}/share?action=off") { auth(me) }
        assertNull(ProjectRepository.byId(project.id)!!.shareToken)
        assertEquals(HttpStatusCode.NotFound, client.get("/share/wrong-token").status)
    }

    @Test
    fun `csv has names and IMAGE formulas, and switches sharing on`() = sheetTest {
        val fern = CharacterRepository.byName("Fern", project.id)!!
        uploadImage(me, fern.id, png(50, 50))
        val olena = ActorRepository.byName("Олена", me.user.id)!!

        val csv = client.get("/tools/sub/${project.id}/sheet.csv?actor=${olena.id}") { auth(me) }
        val token = ProjectRepository.byId(project.id)!!.shareToken
        assertNotNull(token, "CSV needs public image links")
        val lines = csv.bodyAsText().removePrefix("\uFEFF").split("\r\n")
        assertEquals("\"Персонаж\",\"Картинка\"", lines[0])
        assertTrue(lines[1].startsWith("\"Fern\",\"=IMAGE(\"\"https://tools.example.com/share/$token/img/${fern.id}?v="))

        val all = client.get("/share/$token/sheet.csv").bodyAsText().removePrefix("\uFEFF").split("\r\n")
        assertEquals("\"Актор\",\"Персонаж\",\"Картинка\"", all[0])
        assertEquals("\"-\",\"Stark\",\"\"", all.last())
    }

    @Test
    fun `old spreadsheet url redirects`() = sheetTest {
        val r = client.get("/tools/sub/spreadsheet?project=${project.id}&actor=1") { cookie("token", me.token.token) }
        assertEquals("/tools/sub/${project.id}/sheet?actor=1", r.headers[HttpHeaders.Location])
    }

    @Test
    fun `deleting a project deletes its pictures`() = sheetTest {
        val fern = CharacterRepository.byName("Fern", project.id)!!
        uploadImage(me, fern.id, png(20, 20))
        ProjectRepository.delete(project.id)
        assertNull(CharacterImageRepository.get(fern.id))
    }
}

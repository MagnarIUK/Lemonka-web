package com.magnariuk.lemonka.features.subtitles

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
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.test.*

class SubtitlesTest {
    private val ass = """
        [Script Info]
        Title: Ep1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        Dialogue: 0,0:00:01.00,0:00:02.00,Default,Fern,0,0,0,,Привіт
        Dialogue: 0,0:00:02.00,0:00:03.00,Default,Stark,0,0,0,,Здоров
        Dialogue: 0,0:00:03.00,0:00:04.00,Default,Fern та Stark,0,0,0,,Разом
    """.trimIndent()

    private class Ctx(val client: HttpClient, val me: AuthResult.Success, val other: AuthResult.Success)

    private fun subsTest(block: suspend Ctx.() -> Unit) = testApplication {
        val file = Files.createTempFile("lemonka-subs", ".db").toFile().apply { deleteOnExit() }
        environment { config = MapApplicationConfig("lemonka.dbPath" to file.absolutePath) }
        application { module() }
        startApplication()
        val me = AuthService.register("magnar", "12345678", null) as AuthResult.Success
        val other = AuthService.register("other", "12345678", InviteRepository.create(me.user.id).code) as AuthResult.Success
        Ctx(createClient { followRedirects = false }, me, other).block()
    }

    private fun HttpRequestBuilder.auth(s: AuthResult.Success) { cookie("token", s.token.token); header("HX-Request", "true") }

    private suspend fun Ctx.upload(fields: Map<String, String>, name: String = "Ep1.ass", content: String = ass) =
        client.post("/tools/sub") {
            auth(me)
            setBody(MultiPartFormDataContent(formData {
                fields.forEach { (k, v) -> append(k, v) }
                append("file", content.toByteArray(), Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$name\"")
                    append(HttpHeaders.ContentType, "text/plain")
                })
            }))
        }

    @Test
    fun `full flow - new project, actors, assignments, zip export`() = subsTest {
        val r = upload(mapOf("newProject" to "Frieren"))
        val project = ProjectRepository.byName("Frieren", me.user.id)!!
        assertEquals("/tools/sub/${project.id}", r.headers["HX-Redirect"])
        assertEquals(setOf("Fern", "Stark"), CharacterRepository.forProject(project.id).map { it.name }.toSet())
        assertEquals("Ep1.ass", SubtitleFileRepository.get(project.id)?.name)

        // Add actors (adding also puts them in the project)
        client.post("/tools/sub/${project.id}/actors") { auth(me); setBody(FormDataContent(Parameters.build { append("name", "Олена") })) }
        client.post("/tools/sub/${project.id}/actors") { auth(me); setBody(FormDataContent(Parameters.build { append("name", "Максим") })) }
        val olena = ActorRepository.byName("Олена", me.user.id)!!
        val maksym = ActorRepository.byName("Максим", me.user.id)!!
        assertEquals(2, AssignmentRepository.forProject(project.id).size)

        // Assign characters
        val fern = CharacterRepository.byName("Fern", project.id)!!
        val stark = CharacterRepository.byName("Stark", project.id)!!
        client.post("/tools/sub/${project.id}/assign/${olena.id}/${fern.id}") { auth(me); setBody(FormDataContent(Parameters.build { append("checked", "true") })) }
        client.post("/tools/sub/${project.id}/assign/${maksym.id}/${stark.id}") { auth(me); setBody(FormDataContent(Parameters.build { append("checked", "true") })) }

        val page = client.get("/tools/sub/${project.id}") { cookie("token", me.token.token) }.bodyAsText()
        assertContains(page, "Усі персонажі призначені")

        // Export dialog and zip
        assertContains(client.get("/tools/sub/${project.id}/export?kind=ByActor&format=srt") { auth(me) }.bodyAsText(), "Олена.srt")
        val zip = client.get("/tools/sub/${project.id}/download?kind=ByActor&format=srt") { auth(me) }
        assertEquals(ContentType.Application.Zip, zip.contentType()?.withoutParameters())
        assertContains(zip.headers[HttpHeaders.ContentDisposition]!!, "filename*=UTF-8''")
        val entries = ZipInputStream(ByteArrayInputStream(zip.readRawBytes())).use { z ->
            generateSequence { z.nextEntry?.let { it.name to z.readBytes().toString(Charsets.UTF_8) } }.toMap()
        }
        assertEquals(setOf("Олена.srt", "Максим.srt"), entries.keys)
        assertContains(entries["Олена.srt"]!!, "[Fern]: Разом")

        // Single file download
        val one = client.get("/tools/sub/${project.id}/download?kind=Renamed&name=Ep1_renamed.ass") { auth(me) }.bodyAsText()
        assertContains(one, ",Default,Олена та Максим,")

        // Unassign
        client.post("/tools/sub/${project.id}/assign/${olena.id}/${fern.id}") { auth(me); setBody(FormDataContent(Parameters.build { })) }
        assertNull(CharacterRepository.byId(fern.id)!!.actor)
    }

    @Test
    fun `existing project and validation`() = subsTest {
        assertContains(upload(emptyMap()).bodyAsText(), "Оберіть проєкт")
        assertContains(upload(mapOf("newProject" to "X"), name = "x.srt").bodyAsText(), "Потрібен файл .ass")
        val p = ProjectRepository.create("Existing", me.user.id)
        upload(mapOf("project" to "${p.id}"))
        assertEquals(2, CharacterRepository.forProject(p.id).size)
        assertContains(upload(mapOf("newProject" to "Existing")).bodyAsText(), "вже існує")
    }

    @Test
    fun `other users cannot touch my project or use my actors`() = subsTest {
        upload(mapOf("newProject" to "Mine"))
        val p = ProjectRepository.byName("Mine", me.user.id)!!
        ActorRepository.add("TheirActor", other.user.id)
        val theirs = ActorRepository.byName("TheirActor", other.user.id)!!

        assertEquals(HttpStatusCode.NotFound, client.get("/tools/sub/${p.id}") { cookie("token", other.token.token) }.status)
        assertContains(client.get("/tools/sub/${p.id}/download?kind=ByActor") { auth(other) }.bodyAsText(), "Не знайдено")
        // I can't put someone else's actor into my project
        assertContains(client.post("/tools/sub/${p.id}/actors/${theirs.id}/toggle") { auth(me) }.bodyAsText(), "Не знайдено")
        assertTrue(AssignmentRepository.forProject(p.id).isEmpty())
    }

    @Test
    fun `drag and drop - assign, move, unassign, and checks`() = subsTest {
        upload(mapOf("newProject" to "DnD"))
        val p = ProjectRepository.byName("DnD", me.user.id)!!
        for (n in listOf("Олена", "Максим")) client.post("/tools/sub/${p.id}/actors") { auth(me); setBody(FormDataContent(Parameters.build { append("name", n) })) }
        val olena = ActorRepository.byName("Олена", me.user.id)!!
        val maksym = ActorRepository.byName("Максим", me.user.id)!!
        val fern = CharacterRepository.byName("Fern", p.id)!!
        fun drop(item: Int, actor: Int?) = FormDataContent(Parameters.build { append("item", "$item"); actor?.let { append("actor", "$it") } })

        val html = client.post("/tools/sub/${p.id}/drop") { auth(me); setBody(drop(fern.id, olena.id)) }.bodyAsText()
        assertEquals(olena.id, CharacterRepository.byId(fern.id)!!.actor)
        assertContains(html, "id=\"assign-panel\""); assertContains(html, "data-drag=\"${fern.id}\"")

        client.post("/tools/sub/${p.id}/drop") { auth(me); setBody(drop(fern.id, maksym.id)) }   // move
        assertEquals(maksym.id, CharacterRepository.byId(fern.id)!!.actor)
        client.post("/tools/sub/${p.id}/drop") { auth(me); setBody(drop(fern.id, null)) }        // unassign
        assertNull(CharacterRepository.byId(fern.id)!!.actor)

        // Actor not in the project, someone else's actor, someone else's user → rejected
        ActorRepository.add("Ірина", me.user.id)
        val iryna = ActorRepository.byName("Ірина", me.user.id)!!
        assertContains(client.post("/tools/sub/${p.id}/drop") { auth(me); setBody(drop(fern.id, iryna.id)) }.bodyAsText(), "Не знайдено")
        assertContains(client.post("/tools/sub/${p.id}/drop") { auth(other); setBody(drop(fern.id, null)) }.bodyAsText(), "Не знайдено")
        assertNull(CharacterRepository.byId(fern.id)!!.actor)
    }
}

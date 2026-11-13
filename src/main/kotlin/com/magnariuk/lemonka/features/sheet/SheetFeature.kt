package com.magnariuk.lemonka.features.sheet

import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.features.profile.intParam
import com.magnariuk.lemonka.features.profile.ownedProject
import com.magnariuk.lemonka.ui.components.Toast
import com.magnariuk.lemonka.ui.components.ToastTone
import com.magnariuk.lemonka.web.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.FlowContent
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.tbody
import java.security.SecureRandom

/**
 * Character pictures and the project "sheet" (who voices whom, with pictures).
 * The sheet also has a public, read-only version at /share/{token} that needs no login.
 */
object SheetFeature : Feature {
    private val random = SecureRandom()

    override fun Route.publicRoutes() {
        route("/share/{token}") {
            get {
                val project = call.sharedProject()
                val sheet = loadSheet(project)
                val selected = call.selectedActor(sheet)
                val links = publicLinks(call, project)
                call.respondPage(project.name) { SheetPage(sheet, selected, links, inApp = false) }
            }
            get("/img/{character}") {
                val project = call.sharedProject()
                call.respondImage(project, call.intParam("character"))
            }
            get("/sheet.csv") {
                val project = call.sharedProject()
                val sheet = loadSheet(project)
                call.respondCsv(sheet, call.selectedActor(sheet), publicLinks(call, project))
            }
        }
    }

    override fun Route.routes() {
        // Old Vaadin URL: /tools/sub/spreadsheet?project=2&actor=1
        get("/tools/sub/spreadsheet") {
            val p = call.parameters["project"]?.toIntOrNull() ?: throw NotFoundException()
            val actor = call.parameters["actor"]?.let { "?actor=$it" }.orEmpty()
            call.respondRedirect("/tools/sub/$p/sheet$actor")
        }

        route("/tools/sub/{id}") {
            // ---- sheet ----
            get("/sheet") {
                val project = call.ownedProject()
                val sheet = loadSheet(project)
                val links = appLinks(call, project)
                val shareUrl = links.publicUrl(null)
                call.respondPage("Таблиця · ${project.name}") {
                    SheetPage(sheet, call.selectedActor(sheet), links, inApp = true) { ShareBox(project, shareUrl) }
                }
            }
            get("/sheet.csv") {
                var project = call.ownedProject()
                // The CSV contains image links for Google Sheets, which must be public - switch sharing on if needed.
                if (project.shareToken == null) {
                    ProjectRepository.setShareToken(project.id, newToken()); project = call.ownedProject()
                }
                val sheet = loadSheet(project)
                call.respondCsv(sheet, call.selectedActor(sheet), publicLinks(call, project))
            }
            post("/share") {
                val project = call.ownedProject()
                when (call.parameters["action"]) {
                    "on" -> if (project.shareToken == null) ProjectRepository.setShareToken(project.id, newToken())
                    "regen" -> ProjectRepository.setShareToken(project.id, newToken())
                    "off" -> ProjectRepository.setShareToken(project.id, null)
                }
                val updated = call.ownedProject()
                val url = updated.shareToken?.let { "${call.baseUrl()}/share/$it" }
                call.respondFragment {
                    ShareBox(updated, url)
                    Toast(if (url == null) "Публічне посилання вимкнено" else "Посилання оновлено - оновіть сторінку, щоб побачити адреси",
                        if (url == null) ToastTone.Info else ToastTone.Success)
                }
            }

            // ---- image management ----
            get("/images") {
                val project = call.ownedProject()
                val chars = CharacterRepository.forProject(project.id)
                val versions = CharacterImageRepository.versions(chars.map { it.id })
                call.respondFragment { ImagesDialog(project.id, chars, versions) }
            }
            get("/images/rows") {
                val project = call.ownedProject()
                val q = call.parameters["q"].orEmpty().trim()
                val chars = CharacterRepository.forProject(project.id).filter { q.isEmpty() || it.name.contains(q, true) }
                val versions = CharacterImageRepository.versions(chars.map { it.id })
                call.respondRows { ImageRows(project.id, chars, versions) }
            }
            get("/images/{character}") {
                call.respondImage(call.ownedProject(), call.intParam("character"))
            }
            post("/images/{character}") {
                val project = call.ownedProject()
                val character = call.projectCharacter(project)
                val upload = call.receiveUploadForm(Images.MAX_UPLOAD).files.firstOrNull()
                    ?: return@post call.respondToast("Оберіть картинку", ToastTone.Error)
                when (val r = Images.process(upload.bytes, upload.name)) {
                    is Images.Result.Error -> call.respondToast(r.message, ToastTone.Error)
                    is Images.Result.Ok -> {
                        CharacterImageRepository.save(character.id, r.contentType, r.bytes)
                        val v = CharacterImageRepository.versions(listOf(character.id))[character.id]
                        call.respondRows(extra = { Toast("Картинку для «${character.name}» збережено") }) { ImageRow(project.id, character, v) }
                    }
                }
            }
            delete("/images/{character}") {
                val project = call.ownedProject()
                val character = call.projectCharacter(project)
                CharacterImageRepository.delete(character.id)
                call.respondRows { ImageRow(project.id, character, null) }
            }
        }
    }

    // ---------------- helpers ----------------

    private fun newToken() = (1..24).map { "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"[random.nextInt(56)] }.joinToString("")

    private suspend fun ApplicationCall.sharedProject(): Project =
        parameters["token"]?.let { ProjectRepository.byShareToken(it) } ?: throw NotFoundException()

    private suspend fun ApplicationCall.projectCharacter(project: Project): Character =
        CharacterRepository.byId(intParam("character"))?.takeIf { it.project == project.id } ?: throw NotFoundException()

    private fun ApplicationCall.selectedActor(sheet: Sheet): Int? =
        parameters["actor"]?.toIntOrNull()?.takeIf { id -> sheet.actors.any { it.id == id } }

    private suspend fun loadSheet(project: Project): Sheet {
        val chars = CharacterRepository.forProject(project.id)
        val actors = ActorRepository.forUser(project.user).associateBy { it.id }
        val inProject = AssignmentRepository.forProject(project.id).mapNotNull { actors[it.actor] }.sortedBy { it.name.lowercase() }
        val ids = inProject.map { it.id }.toSet()
        val groups = inProject.map { a -> a to chars.filter { it.actor == a.id }.sortedBy { it.name.lowercase() } } +
            listOf(null to chars.filter { it.actor == null || it.actor !in ids }.sortedBy { it.name.lowercase() })
        return Sheet(project, groups, CharacterImageRepository.versions(chars.map { it.id }))
    }

    private fun appLinks(call: ApplicationCall, project: Project): SheetLinks {
        val base = "/tools/sub/${project.id}"
        val public = project.shareToken?.let { "${call.baseUrl()}/share/$it" }
        return SheetLinks(
            page = { a -> "$base/sheet" + (a?.let { "?actor=$it" } ?: "") },
            image = { c, v -> "$base/images/$c?v=$v" },
            csv = { a -> "$base/sheet.csv" + (a?.let { "?actor=$it" } ?: "") },
            publicUrl = { a -> public?.let { it + (a?.let { id -> "?actor=$id" } ?: "") } },
        )
    }

    private fun publicLinks(call: ApplicationCall, project: Project): SheetLinks {
        val path = "/share/${project.shareToken}"
        val full = "${call.baseUrl()}$path"
        return SheetLinks(
            page = { a -> path + (a?.let { "?actor=$it" } ?: "") },
            image = { c, v -> "$path/img/$c?v=$v" },
            csv = { a -> "$path/sheet.csv" + (a?.let { "?actor=$it" } ?: "") },
            publicUrl = { a -> full + (a?.let { "?actor=$it" } ?: "") },
        )
    }

    private suspend fun ApplicationCall.respondImage(project: Project, characterId: Int) {
        val character = CharacterRepository.byId(characterId)?.takeIf { it.project == project.id } ?: throw NotFoundException()
        val image = CharacterImageRepository.get(character.id) ?: throw NotFoundException()
        response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable") // URL has ?v=
        respondBytes(image.bytes, ContentType.parse(image.contentType))
    }

    /**
     * CSV for Google Sheets: character name + `=IMAGE("…")` so pictures show up after import.
     * The whole-project export also has an actor column.
     */
    private suspend fun ApplicationCall.respondCsv(sheet: Sheet, selected: Int?, links: SheetLinks) {
        val base = baseUrl()
        val groups = if (selected == null) sheet.groups else sheet.groups.filter { it.first?.id == selected }
        val rows = buildList {
            add(if (selected == null) listOf("Актор", "Персонаж", "Картинка") else listOf("Персонаж", "Картинка"))
            groups.forEach { (actor, chars) ->
                chars.forEach { c ->
                    val img = sheet.versions[c.id]?.let { v -> "=IMAGE(\"$base${links.image(c.id, v)}\")" }.orEmpty()
                    add(if (selected == null) listOf(actor?.name ?: "-", c.name, img) else listOf(c.name, img))
                }
            }
        }
        val csv = "\uFEFF" + rows.joinToString("\r\n") { r -> r.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } }
        val who = selected?.let { id -> sheet.actors.first { it.id == id }.name } ?: "усі"
        val name = "${sheet.project.name} - $who.csv"
        val ascii = name.map { if (it.code in 32..126 && it != '"') it else '_' }.joinToString("")
        response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$ascii\"; filename*=UTF-8''${name.encodeURLParameter()}")
        respondText(csv, ContentType.Text.CSV.withCharset(Charsets.UTF_8))
    }

    /** Responds with table rows (a `<tbody>` fragment's children). */
    private suspend fun ApplicationCall.respondRows(extra: (FlowContent.() -> Unit)? = null, rows: kotlinx.html.TBODY.() -> Unit) {
        val html = createHTML(prettyPrint = false).table { tbody { rows() } }
            .removePrefix("<table><tbody>").removeSuffix("</tbody></table>")
        val toast = extra?.let { fragment(it) }.orEmpty()
        respondText(html + toast, ContentType.Text.Html.withCharset(Charsets.UTF_8))
    }
}

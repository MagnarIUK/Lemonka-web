package com.magnariuk.lemonka.features.subtitles

import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.features.profile.intParam
import com.magnariuk.lemonka.features.profile.ownedProject
import com.magnariuk.lemonka.subs.*
import com.magnariuk.lemonka.ui.components.Icon
import com.magnariuk.lemonka.ui.components.Toast
import com.magnariuk.lemonka.ui.components.ToastTone
import com.magnariuk.lemonka.ui.layout.NavItem
import com.magnariuk.lemonka.web.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Subtitle splitting (old SubsToolView): assign actors to characters and export per-actor files. */
object SubtitlesFeature : Feature {
    const val PATH = "/tools/sub"
    private const val MAX_UPLOAD = 50L * 1024 * 1024

    override val nav = NavItem("Розділення субтитрів", PATH, Icon.Subtitles)

    override fun Route.routes() {
        route(PATH) {
            get {
                val projects = ProjectRepository.forUser(call.user.id)
                val files = SubtitleFileRepository.names(projects.map { it.id })
                call.respondPage("Розділення субтитрів") { StartPage(projects, files) }
            }

            // Upload + choose or create the project, then go to its workspace.
            post {
                val form = call.receiveUploadForm(MAX_UPLOAD)
                val user = call.user
                val newName = form["newProject"].orEmpty().trim()
                val file = form.files.firstOrNull() ?: return@post call.respondToast("Завантажте файл .ass", ToastTone.Error)
                if (file.extension != "ass") return@post call.respondToast("Потрібен файл .ass", ToastTone.Error)
                val project = when {
                    newName.isNotEmpty() -> {
                        if (ProjectRepository.byName(newName, user.id) != null)
                            return@post call.respondToast("Проєкт з такою назвою вже існує", ToastTone.Error)
                        ProjectRepository.create(newName, user.id)
                    }
                    else -> form["project"]?.toIntOrNull()?.let { call.ownedProject(it) }
                        ?: return@post call.respondToast("Оберіть проєкт або введіть назву нового", ToastTone.Error)
                }
                importFile(project.id, file)
                call.hxRedirect("$PATH/${project.id}")
            }

            route("/{id}") {
                get {
                    val w = workspace(call)
                    call.respondPage(w.project.name) { WorkspacePage(w) }
                }
                post("/file") {
                    val project = call.ownedProject()
                    val file = call.receiveUploadForm(MAX_UPLOAD).files.firstOrNull()
                        ?: return@post call.respondToast("Оберіть файл", ToastTone.Error)
                    if (file.extension != "ass") return@post call.respondToast("Потрібен файл .ass", ToastTone.Error)
                    importFile(project.id, file)
                    call.hxRedirect("$PATH/${project.id}")
                }

                // ---- actors ----
                get("/actors") {
                    val w = workspace(call)
                    val q = call.parameters["name"].orEmpty()
                    call.respondFragment { ActorRows(w, q) }
                }
                post("/actors") {
                    val project = call.ownedProject()
                    val name = call.receiveParameters()["name"].orEmpty().trim()
                    if (name.isEmpty()) return@post call.respondToast("Введіть ім'я актора", ToastTone.Error)
                    val existing = ActorRepository.byName(name, call.user.id)
                    if (existing == null) ActorRepository.add(name, call.user.id)
                    val actor = existing ?: ActorRepository.byName(name, call.user.id)!!
                    // Adding from here also puts the actor into this project - that's what you want 9 times out of 10.
                    if (AssignmentRepository.find(project.id, actor.id) == null) AssignmentRepository.add(actor.id, project.id)
                    call.respondPanels("", if (existing == null) "Актора «$name» додано" else "«$name» додано в проєкт")
                }
                post("/actors/{actor}/toggle") {
                    val project = call.ownedProject()
                    val actor = call.ownedActor()
                    val a = AssignmentRepository.find(project.id, actor.id)
                    if (a == null) AssignmentRepository.add(actor.id, project.id) else AssignmentRepository.remove(a.id)
                    call.respondPanels(formQuery(call))
                }
                delete("/actors/{actor}") {
                    call.ownedProject()
                    val actor = call.ownedActor()
                    ActorRepository.remove(actor.id)
                    call.respondPanels(formQuery(call), "Актора «${actor.name}» видалено")
                }

                // ---- character assignment ----
                post("/hide-selected") {
                    SettingsRepository.setHideSelected(call.user.id, call.receiveParameters()["hideSelected"] == "true")
                    call.respondPanels("")
                }
                get("/assign/{actor}") {
                    val w = workspace(call)
                    val actor = w.assignedActors.firstOrNull { it.id == call.intParam("actor") } ?: throw NotFoundException()
                    call.respondFragment { AssignDialog(w, actor) }
                }
                post("/assign/{actor}/{character}") {
                    val project = call.ownedProject()
                    val actor = call.ownedActor()
                    val character = CharacterRepository.byId(call.intParam("character"))?.takeIf { it.project == project.id }
                        ?: throw NotFoundException()
                    val checked = call.receiveParameters()["checked"] == "true"
                    CharacterRepository.assign(character.id, if (checked) actor.id else null)
                    call.respondPanels("", toast = null, onlyAssignments = true)
                }

                // Drag & drop: `item` = character id, `actor` = actor id (missing = unassign).
                post("/drop") {
                    val project = call.ownedProject()
                    val f = call.receiveParameters()
                    val character = f["item"]?.toIntOrNull()?.let { CharacterRepository.byId(it) }
                        ?.takeIf { it.project == project.id } ?: throw NotFoundException()
                    val actorId = f["actor"]?.toIntOrNull()
                    if (actorId != null) {
                        // Only actors of this user that are in this project.
                        val ok = ActorRepository.byId(actorId)?.user == call.user.id && AssignmentRepository.find(project.id, actorId) != null
                        if (!ok) throw NotFoundException()
                    }
                    if (character.actor != actorId) CharacterRepository.assign(character.id, actorId)
                    call.respondPanels("", onlyAssignments = true)
                }

                // ---- export ----
                get("/export") {
                    val (kind, format, files) = exports(call)
                    val id = call.intParam()
                    call.respondFragment { ExportDialog(id, kind, format, files) }
                }
                get("/download") {
                    val (kind, format, files) = exports(call)
                    val name = call.parameters["name"]
                    val project = call.ownedProject()
                    if (name != null) {
                        val f = files.firstOrNull { it.name == name } ?: throw NotFoundException()
                        call.attachment(f.name)
                        call.respondText(f.content, ContentType.Text.Plain.withCharset(Charsets.UTF_8))
                    } else {
                        val suffix = when (kind) { ExportKind.ByActor -> "актори"; ExportKind.ByCharacter -> "персонажі"; ExportKind.Renamed -> "перейменовано" }
                        call.attachment("${project.name} - $suffix (${format.ext}).zip")
                        call.respondBytes(SubtitleExporter.zip(files), ContentType.Application.Zip)
                    }
                }
            }
        }
    }

    // ---------------- helpers ----------------

    /** Stores the file and adds every character in it to the project (split by the separators). */
    private suspend fun importFile(project: Int, file: UploadedFile) {
        val name = file.name
        val text = file.bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val separators = SeparatorRepository.all()
        val names = AssParser.parse(text.byteInputStream(), name).events.dialogues
            .flatMap { ActorNames.split(it.actor, separators) }.distinct()
        CharacterRepository.addMissing(names, project)
        SubtitleFileRepository.save(project, name, text)
    }

    private suspend fun workspace(call: ApplicationCall): Workspace {
        val project = call.ownedProject()
        val actors = ActorRepository.forUser(call.user.id)
        val assigned = AssignmentRepository.forProject(project.id).map { it.actor }.toSet()
        return Workspace(
            project = project,
            file = SubtitleFileRepository.get(project.id),
            actors = actors,
            assignedActors = actors.filter { it.id in assigned },
            characters = CharacterRepository.forProject(project.id),
            hideSelected = SettingsRepository.get(call.user.id).hideSelected,
        )
    }

    /** Re-renders the actor and assignment panels in place (out-of-band), with an optional toast. */
    private suspend fun ApplicationCall.respondPanels(query: String, toast: String? = null, onlyAssignments: Boolean = false) {
        val w = workspace(this)
        respondFragment {
            if (!onlyAssignments) ActorsPanel(w, query, oob = true)
            AssignmentsPanel(w, oob = true)
            if (toast != null) Toast(toast)
        }
    }

    /** The actor search text, sent along via hx-include (in the body for POST, in the query for DELETE). */
    private suspend fun formQuery(call: ApplicationCall): String =
        call.request.queryParameters["name"] ?: runCatching { call.receiveParameters()["name"] }.getOrNull().orEmpty()

    private suspend fun ApplicationCall.ownedActor(): Actor =
        ActorRepository.byId(intParam("actor"))?.takeIf { it.user == user.id } ?: throw NotFoundException()

    private suspend fun exports(call: ApplicationCall): Triple<ExportKind, SubFormat, List<ExportFile>> {
        val project = call.ownedProject()
        val file = SubtitleFileRepository.get(project.id) ?: throw NotFoundException()
        val kind = ExportKind.of(call.parameters["kind"])
        val format = if (kind == ExportKind.Renamed) SubFormat.Ass else SubFormat.of(call.parameters["format"])
        val actors = ActorRepository.forUser(call.user.id).associateBy { it.id }
        val mapping = AssignmentRepository.forProject(project.id).mapNotNull { a ->
            val actor = actors[a.actor] ?: return@mapNotNull null
            actor.name to CharacterRepository.forActor(actor.id, project.id).map { it.name }
        }.toMap()
        val files = SubtitleExporter(file.name, file.content, SeparatorRepository.all()).export(kind, format, mapping)
        return Triple(kind, format, files)
    }

    private fun ApplicationCall.attachment(name: String) {
        val ascii = name.map { if (it.code in 32..126 && it != '"') it else '_' }.joinToString("")
        response.header(HttpHeaders.ContentDisposition,
            "attachment; filename=\"$ascii\"; filename*=UTF-8''${name.encodeURLParameter()}")
    }
}

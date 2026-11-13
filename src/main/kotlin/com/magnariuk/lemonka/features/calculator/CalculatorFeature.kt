package com.magnariuk.lemonka.features.calculator

import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.data.db.CharacterRepository
import com.magnariuk.lemonka.data.db.ProjectRepository
import com.magnariuk.lemonka.data.db.SeparatorRepository
import com.magnariuk.lemonka.features.profile.ownedProject
import com.magnariuk.lemonka.subs.*
import com.magnariuk.lemonka.ui.components.Icon
import com.magnariuk.lemonka.ui.components.ToastTone
import com.magnariuk.lemonka.ui.layout.NavItem
import com.magnariuk.lemonka.web.*
import io.ktor.server.routing.*

/** Word calculator (old ActorMeter): count lines, words and numbers for chosen characters. */
object CalculatorFeature : Feature {
    const val PATH = "/tools/calculator"
    private const val MAX_UPLOAD = 50L * 1024 * 1024

    override val nav = NavItem("Калькулятор слів", PATH, Icon.Calculator)

    override fun Route.routes() {
        get(PATH) {
            val projects = ProjectRepository.forUser(call.user.id)
            call.respondPage("Калькулятор слів") { CalculatorPage(projects) }
        }
        // Character checklist for the chosen project.
        get("$PATH/characters") {
            val id = call.parameters["project"]?.toIntOrNull()
            val names = id?.let { CharacterRepository.forProject(call.ownedProject(it).id).map { c -> c.name } }.orEmpty()
            call.respondFragment { CharacterPicker(names, projectChosen = id != null) }
        }
        post(PATH) {
            val form = call.receiveUploadForm(MAX_UPLOAD)
            val rejected = form.files.filter { it.extension !in setOf("srt", "ass") }.map { it.name }
            val all = form["all"] == "true"
            val project = form["project"]?.toIntOrNull()
            val error = when {
                form.files.isEmpty() -> "Завантажте субтитри (.srt або .ass)"
                rejected.isNotEmpty() -> "Потрібен формат srt або ass: ${rejected.joinToString()}"
                !all && form.all("character").isEmpty() -> "Оберіть персонажів або увімкніть «усіх персонажів»"
                else -> null
            }
            if (error != null) return@post call.respondToast(error, ToastTone.Error)

            // Only characters from the user's own project count (ignore anything posted for other projects).
            val allowed = project?.let { CharacterRepository.forProject(call.ownedProject(it).id).map { c -> c.name }.toSet() }
            val characters = if (all) emptySet() else form.all("character").filter { allowed == null || it in allowed }.toSet()
            val separators = SeparatorRepository.all()
            val counts = form.files.map { f ->
                val lines = when (f.extension) {
                    "srt" -> WordCounter.linesOf(SrtParser.parse(f.bytes.inputStream(), f.name), separators)
                    else -> WordCounter.linesOf(AssParser.parse(f.bytes.inputStream(), f.name), separators)
                }
                WordCounter.count(f.name, lines, characters)
            }
            val report = CountReport(characters.sorted(), counts)
            call.respondFragment { CalculatorResult(report) }
        }
    }
}

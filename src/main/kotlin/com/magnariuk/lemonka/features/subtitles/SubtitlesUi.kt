package com.magnariuk.lemonka.features.subtitles

import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.subs.ExportFile
import com.magnariuk.lemonka.subs.ExportKind
import com.magnariuk.lemonka.subs.SubFormat
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import com.magnariuk.lemonka.web.formatDate
import com.magnariuk.lemonka.web.plural
import io.ktor.http.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

private const val P = SubtitlesFeature.PATH

// ---------------- start page ----------------

fun FlowContent.StartPage(projects: List<Project>, files: Map<Int, String>, error: String? = null) {
    div("page-title") {
        h1 { +"Розділення субтитрів" }
        p("muted") { +"Завантажте .ass, призначте акторам персонажів - і отримайте окремі файли для кожного." }
    }
    div("calc-grid") {
        Card("Новий файл") {
            form {
                hxPost(P); hxEncoding("multipart/form-data"); hxTarget("this"); hxSwap("none")
                Column(gap = 16) {
                    Upload("file", ".ass", "Файл Advanced SubStation (.ass)", multiple = false)
                    Select("Проєкт", "project", projects.map { Option("${it.id}", it.name) },
                        placeholder = if (projects.isEmpty()) "Проєктів ще немає - створіть новий нижче" else "Оберіть існуючий проєкт")
                    TextField("…або новий проєкт", "newProject", placeholder = "Назва нового проєкту", error = error)
                    Button("Продовжити", ButtonVariant.Primary, icon = Icon.Subtitles, type = ButtonType.submit)
                }
            }
        }
        Card("Продовжити роботу") {
            if (projects.isEmpty()) EmptyState("Тут з'являться ваші проєкти", Icon.Subtitles)
            else div("project-list") {
                projects.sortedByDescending { it.id }.forEach { p ->
                    a("$P/${p.id}", classes = "project-link") {
                        span("tile-icon") { Icon(Icon.Subtitles) }
                        div("grow") {
                            b { +p.name }
                            div("muted small") { +(files[p.id] ?: "файл не завантажено") }
                        }
                        Icon(Icon.ChevronDown, "chev")
                    }
                }
            }
        }
    }
}

// ---------------- workspace ----------------

class Workspace(
    val project: Project,
    val file: SubtitleFile?,
    val actors: List<Actor>,
    val assignedActors: List<Actor>,
    val characters: List<Character>,
    val hideSelected: Boolean,
) {
    val assignedIds = assignedActors.map { it.id }.toSet()
    fun charactersOf(actor: Int) = characters.filter { it.actor == actor }.sortedBy { it.name.lowercase() }
    val unassigned get() = characters.filter { it.actor == null || it.actor !in assignedIds }.sortedBy { it.name.lowercase() }
}

fun FlowContent.WorkspacePage(w: Workspace) {
    div("ws-head") {
        div {
            a(P, classes = "muted small back") { +"← Усі проєкти" }
            h1 { +w.project.name }
            div("ws-tools") {
                Button("Картинки персонажів", small = true, icon = Icon.Edit) {
                    hxGet("$P/${w.project.id}/images"); hxTarget("#modal")
                }
                LinkButton("Таблиця для акторів", "$P/${w.project.id}/sheet", icon = Icon.User)
            }
        }
        FileBox(w)
    }
    div("ws-grid") {
        ActorsPanel(w, "")
        AssignmentsPanel(w)
    }
    ExportCard(w)
}

private fun FlowContent.FileBox(w: Workspace) = form(classes = "file-box") {
    hxPost("$P/${w.project.id}/file"); hxEncoding("multipart/form-data"); hxTrigger("change"); hxSwap("none")
    uploadDropZone()
    span("tile-icon") { Icon(Icon.Subtitles) }
    div("grow") {
        if (w.file != null) {
            b { +w.file.name }
            div("muted small") { +"завантажено ${formatDate(w.file.uploadedAt)} · ${plural(w.characters.size, "персонаж", "персонажі", "персонажів")}" }
        } else {
            b { +"Файл не завантажено" }
            div("muted small") { +"Без файлу можна налаштувати акторів, але не експортувати" }
        }
    }
    label("btn btn-secondary btn-small") {
        Icon(Icon.Upload); span { +if (w.file == null) "Завантажити .ass" else "Замінити" }
        input(InputType.file, name = "file", classes = "visually-hidden") { accept = ".ass" }
    }
}

/** All of the user's actors; + / − adds or removes them from this project. */
fun FlowContent.ActorsPanel(w: Workspace, query: String, oob: Boolean = false) =
    Card("Актори", id = "actors-panel", attrs = { if (oob) hxSwapOob("outerHTML") }) {
        form {
            hxPost("$P/${w.project.id}/actors"); hxSwap("none")
            Row(gap = 8, align = Alignment.Start) {
                div("grow") {
                    TextField(null, "name", query, placeholder = "Пошук або новий актор…") {
                        id = "actor-search"
                        hxGet("$P/${w.project.id}/actors"); hxTrigger("input changed delay:200ms")
                        hxTarget("#actor-rows"); hxSwap("innerHTML"); attributes["hx-params"] = "name"
                        attributes["autocomplete"] = "off"
                    }
                }
                Button(icon = Icon.Plus, variant = ButtonVariant.Primary, type = ButtonType.submit) {
                    attributes["aria-label"] = "Додати актора"; attributes["title"] = "Додати актора"
                }
            }
        }
        div { id = "actor-rows"; ActorRows(w, query) }
    }

/** The (filtered) list of actors inside [ActorsPanel]. */
fun FlowContent.ActorRows(w: Workspace, query: String) {
        val shown = w.actors.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareBy({ it.id !in w.assignedIds }, { it.name.lowercase() }))
        if (shown.isEmpty()) {
            EmptyState(if (w.actors.isEmpty()) "Додайте першого актора" else "Нікого не знайдено", Icon.User)
            return
        }
        div("actor-list") {
            shown.forEach { a ->
                val inProject = a.id in w.assignedIds
                div("actor-row${if (inProject) " in-project" else ""}") {
                    span("grow") { +a.name }
                    Button(small = true, variant = ButtonVariant.Tertiary, icon = Icon.Trash) {
                        attributes["aria-label"] = "Видалити актора"
                        hxDelete("$P/${w.project.id}/actors/${a.id}"); hxSwap("none"); hxInclude("#actor-search")
                        hxConfirm("Видалити актора «${a.name}» з усіх проєктів?")
                    }
                    Button(small = true, variant = if (inProject) ButtonVariant.Secondary else ButtonVariant.Primary,
                        icon = if (inProject) Icon.Close else Icon.Plus) {
                        attributes["aria-label"] = if (inProject) "Прибрати з проєкту" else "Додати в проєкт"
                        attributes["title"] = if (inProject) "Прибрати з проєкту" else "Додати в проєкт"
                        hxPost("$P/${w.project.id}/actors/${a.id}/toggle"); hxSwap("none"); hxInclude("#actor-search")
                    }
                }
            }
        }
}

/** Actors in this project with their characters. */
fun FlowContent.AssignmentsPanel(w: Workspace, oob: Boolean = false) =
    Card("Хто кого озвучує", id = "assign-panel", attrs = { if (oob) hxSwapOob("outerHTML") }, actions = {
        Switch("Ховати вже призначених", "hideSelected", w.hideSelected) {
            hxPost("$P/${w.project.id}/hide-selected"); hxTrigger("change"); hxSwap("none")
        }
    }) {
        if (w.assignedActors.isEmpty()) {
            EmptyState("Додайте акторів у проєкт кнопкою + зліва", Icon.User)
        } else div("assign-list") {
            w.assignedActors.sortedBy { it.name.lowercase() }.forEach { a ->
                val chars = w.charactersOf(a.id)
                div("assign-row") {
                    dropZone("$P/${w.project.id}/drop", "actor" to a.id)
                    div("assign-actor") {
                        span("avatar") { +a.name.take(1).uppercase() }
                        b { +a.name }
                    }
                    div("chips grow") {
                        if (chars.isEmpty()) span("muted small") { +"перетягніть сюди персонажів" }
                        chars.forEach { span("chip") { dragItem("${it.id}"); +it.name } }
                    }
                    Button("Персонажі", small = true, icon = Icon.Edit) {
                        hxGet("$P/${w.project.id}/assign/${a.id}"); hxTarget("#modal")
                    }
                }
            }
        }
        val left = w.unassigned
        // Also a drop zone: drag a character here to take it away from its actor.
        if (w.characters.isNotEmpty()) div("unassigned") {
            dropZone("$P/${w.project.id}/drop")
            if (left.isEmpty()) {
                Badge("Усі персонажі призначені", BadgeTone.Success)
                if (w.assignedActors.isNotEmpty()) span("muted small") { +"Перетягніть персонажа сюди, щоб зняти призначення" }
                return@div
            }
            span("muted small") { +"Без актора (${left.size}): " }
            left.forEach { span("chip chip-muted") { dragItem("${it.id}"); +it.name } }
            if (w.assignedActors.isNotEmpty()) span("muted small drag-hint") { +"- перетягніть на актора" }
        }
    }

/** Checklist of characters for one actor. Each tick saves immediately. */
fun FlowContent.AssignDialog(w: Workspace, actor: Actor) = Dialog("Персонажі: ${actor.name}") {
    val own = w.charactersOf(actor.id).map { it.id }.toSet()
    val options = w.characters
        .filter { !w.hideSelected || it.actor == null || it.actor == actor.id || it.actor !in w.assignedIds }
        .sortedWith(compareBy({ it.id !in own }, { it.name.lowercase() }))
    if (options.isEmpty()) { EmptyState("У проєкті немає вільних персонажів"); return@Dialog }
    p("muted small") { +"Позначки зберігаються одразу." }
    div("char-scroll char-scroll-tall") {
        options.forEach { c ->
            val other = c.actor?.takeIf { it != actor.id && it in w.assignedIds }?.let { id -> w.assignedActors.first { it.id == id }.name }
            label("check") {
                input(InputType.checkBox, name = "checked") {
                    value = "true"; checked = c.id in own
                    hxPost("$P/${w.project.id}/assign/${actor.id}/${c.id}"); hxTrigger("change"); hxSwap("none")
                }
                span("grow") { +c.name }
                if (other != null) span("muted small") { +"зараз: $other" }
            }
        }
    }
    Row(arrange = Arrangement.End, classes = "dialog-actions") { DialogCloseButton("Готово") }
}

fun FlowContent.ExportCard(w: Workspace) = Card("Отримати файли") {
    if (w.file == null) { EmptyState("Завантажте .ass файл угорі, щоб експортувати", Icon.Upload); return@Card }
    form {
        id = "export-form"
        div("export-grid") {
            Select("Формат", "format", SubFormat.entries.map { Option(it.ext, it.title) }, selected = SubFormat.Ass.ext)
            ExportButton(w, ExportKind.ByActor, ButtonVariant.Primary, "Створити субтитри")
            ExportButton(w, ExportKind.ByCharacter, ButtonVariant.Secondary, "Розділити по персонажах")
            ExportButton(w, ExportKind.Renamed, ButtonVariant.Secondary, "Перейменувати акторів")
        }
    }
}

private fun FlowContent.ExportButton(w: Workspace, kind: ExportKind, variant: ButtonVariant, text: String) =
    Button(text, variant, icon = Icon.Download) {
        hxGet("$P/${w.project.id}/export?kind=${kind.name}"); hxInclude("#export-form"); hxTarget("#modal")
    }

fun FlowContent.ExportDialog(projectId: Int, kind: ExportKind, format: SubFormat, files: List<ExportFile>) =
    Dialog(kind.title) {
        if (files.isEmpty()) {
            EmptyState("Немає жодної репліки для призначених персонажів"); return@Dialog
        }
        val q = "kind=${kind.name}&format=${format.ext}"
        div("export-files") {
            files.forEach { f ->
                a("$P/$projectId/download?$q&name=${f.name.encodeURLParameter()}", classes = "export-file") {
                    hxBoost(false); attributes["download"] = f.name
                    Icon(Icon.Download)
                    span("grow") { +f.name }
                    span("muted small") { +plural(f.lines, "репліка", "репліки", "реплік") }
                }
            }
        }
        if (files.size > 1) Row(arrange = Arrangement.End, classes = "dialog-actions") {
            a("$P/$projectId/download?$q", classes = "btn btn-primary") {
                hxBoost(false); attributes["download"] = ""
                Icon(Icon.Download); span { +"Завантажити все (.zip)" }
            }
        }
    }

object SubtitlesStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".project-list") { display = Display.flex; flexDirection = FlexDirection.column; gap = 6.px }
        rule(".project-link") {
            display = Display.flex; alignItems = Align.center; gap = 12.px; padding = Padding(10.px)
            borderRadius = LinearDimension(v("radius-m")); color = tc("text")
            put("transition", "background-color .15s, transform .15s ${v("ease")}")
        }
        rule(".project-link:hover") { backgroundColor = tc("contrast-5"); textDecoration = TextDecoration.none; transform { translateX(2.px) } }
        rule(".project-link .tile-icon") { width = 38.px; height = 38.px; fontSize = 18.px }
        rule(".project-link .chev") { transform { rotate((-90).deg) }; color = tc("text-muted") }

        rule(".ws-head") { display = Display.flex; alignItems = Align.end; gap = 16.px; marginBottom = 18.px; flexWrap = FlexWrap.wrap }
        rule(".ws-head > div:first-child") { flexGrow = 1.0 }
        rule(".back") { display = Display.inlineBlock; marginBottom = 4.px }
        rule(".file-box") {
            display = Display.flex; alignItems = Align.center; gap = 12.px; minWidth = 320.px
            padding = Padding(10.px, 12.px); backgroundColor = tc("base"); borderRadius = LinearDimension(v("radius-m"))
            put("box-shadow", v("shadow-s"))
        }
        rule(".file-box.drag-over") { put("box-shadow", "0 0 0 2px ${v("primary")}"); backgroundColor = tc("primary-10") }
        rule(".file-box .tile-icon") { width = 38.px; height = 38.px; fontSize = 18.px }
        rule(".visually-hidden") { position = Position.absolute; width = 1.px; height = 1.px; opacity = 0; overflow = Overflow.hidden }

        rule(".ws-grid") {
            display = Display.grid; gap = 16.px; alignItems = Align.start; marginBottom = 16.px
            put("grid-template-columns", "minmax(260px, 1fr) minmax(0, 2fr)")
        }
        media("(max-width: 900px)") { rule(".ws-grid") { put("grid-template-columns", "1fr") } }

        rule(".actor-list") { display = Display.flex; flexDirection = FlexDirection.column; gap = 2.px; marginTop = 10.px; maxHeight = 520.px; overflowY = Overflow.auto }
        rule(".actor-row") {
            display = Display.flex; alignItems = Align.center; gap = 6.px
            padding = Padding(4.px, 4.px, 4.px, 10.px); borderRadius = LinearDimension(v("radius-s"))
            put("transition", "background-color .15s")
        }
        rule(".actor-row:hover") { backgroundColor = tc("contrast-5") }
        rule(".actor-row.in-project") { backgroundColor = tc("primary-10"); fontWeight = FontWeight.w500 }

        rule(".assign-list") { display = Display.flex; flexDirection = FlexDirection.column }
        rule(".assign-row") {
            display = Display.flex; alignItems = Align.center; gap = 12.px; padding = Padding(10.px, 4.px)
            put("border-bottom", "1px solid ${v("contrast-5")}")
            put("animation", "fade-in .25s ${v("ease")}")
        }
        rule(".assign-actor") { display = Display.flex; alignItems = Align.center; gap = 8.px; width = 170.px; flexShrink = 0.0 }
        rule(".assign-row .chips") { margin = Margin(0.px); gap = 4.px }
        rule(".chip-muted") { backgroundColor = tc("contrast-10"); color = tc("text-secondary") }
        rule(".unassigned") {
            display = Display.flex; flexWrap = FlexWrap.wrap; gap = 4.px; alignItems = Align.center
            marginTop = 14.px; padding = Padding(8.px); minHeight = 44.px
        }
        rule(".assign-row") { paddingLeft = 8.px; paddingRight = 8.px }
        rule(".char-scroll-tall") { maxHeight = 52.vh }
        rule(".dialog-actions") { marginTop = 14.px }

        rule(".export-grid") {
            display = Display.grid; gap = 12.px; alignItems = Align.end
            put("grid-template-columns", "minmax(220px, 1.4fr) repeat(3, auto)")
        }
        media("(max-width: 900px)") { rule(".export-grid") { put("grid-template-columns", "1fr") } }
        rule(".export-files") { display = Display.flex; flexDirection = FlexDirection.column; gap = 4.px }
        rule(".export-file") {
            display = Display.flex; alignItems = Align.center; gap = 10.px; padding = Padding(10.px, 12.px)
            borderRadius = LinearDimension(v("radius-s")); backgroundColor = tc("contrast-5"); color = tc("text")
            put("transition", "background-color .15s")
        }
        rule(".export-file:hover") { backgroundColor = tc("primary-10"); textDecoration = TextDecoration.none; color = tc("primary-text") }
    }
}

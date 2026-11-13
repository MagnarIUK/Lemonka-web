package com.magnariuk.lemonka.features.sheet

import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import com.magnariuk.lemonka.web.plural
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** Everything a project sheet shows. Built the same way for the in-app and the public page. */
class Sheet(
    val project: Project,
    val groups: List<Pair<Actor?, List<Character>>>, // null actor = characters nobody voices yet
    val versions: Map<Int, Long>,
) {
    val actors get() = groups.mapNotNull { it.first }
}

/**
 * Where links on the sheet point. In the app they go through the logged-in routes;
 * on the public page through /share/{token}.
 */
class SheetLinks(
    val page: (actorId: Int?) -> String,
    val image: (characterId: Int, version: Long) -> String,
    val csv: (actorId: Int?) -> String,
    /** Full public address for an actor (or the whole project), or null if sharing is off. */
    val publicUrl: (actorId: Int?) -> String?,
)

fun FlowContent.SheetPage(sheet: Sheet, selected: Int?, links: SheetLinks, inApp: Boolean, shareBox: (FlowContent.() -> Unit)? = null) {
    div("sheet-head") {
        div {
            if (inApp) a("/tools/sub/${sheet.project.id}", classes = "muted small back") { +"← До проєкту" }
            div("muted") { +"Проєкт" }
            h1 { +sheet.project.name }
        }
        shareBox?.invoke(this)
    }

    nav("pills") {
        a(links.page(null), classes = "pill${if (selected == null) " active" else ""}") { +"Усі актори" }
        sheet.actors.forEach { a ->
            a(links.page(a.id), classes = "pill${if (selected == a.id) " active" else ""}") { +a.name }
        }
    }

    val groups = if (selected == null) sheet.groups else sheet.groups.filter { it.first?.id == selected }
    if (groups.all { it.second.isEmpty() }) {
        Card { EmptyState("Тут поки що нема персонажів", Icon.User) }
    }
    groups.filter { it.second.isNotEmpty() }.forEach { (actor, chars) ->
        Card(classes = "sheet-card") {
            div("sheet-group") {
                if (actor != null) span("avatar") { +actor.name.take(1).uppercase() }
                h2 { +(actor?.name ?: "Без актора") }
                span("muted small") { +plural(chars.size, "персонаж", "персонажі", "персонажів") }
            }
            Table(listOf("Ім'я персонажа", "Картинка")) {
                chars.forEach { c ->
                    tr {
                        td("sheet-name") { +c.name }
                        td {
                            val v = sheet.versions[c.id]
                            if (v != null) a(links.image(c.id, v), target = "_blank", classes = "sheet-img-link") {
                                hxBoost(false)
                                img(c.name, links.image(c.id, v), classes = "sheet-img") { attributes["loading"] = "lazy" }
                            } else span("muted small") { +"немає картинки" }
                        }
                    }
                }
            }
        }
    }

    div("sheet-actions") {
        val url = links.publicUrl(selected)
        if (url != null) {
            label("field share-field") {
                span("field-label") { Icon(Icon.Edit); +(if (selected == null) " Посилання на проєкт" else " Посилання на актора") }
                input(InputType.text, classes = "input link-input") { readonly = true; value = url }
            }
        }
        a(links.csv(selected), classes = "btn btn-primary btn-sheets") {
            hxBoost(false); attributes["download"] = ""
            Icon(Icon.Download); span { +"Експорт в CSV (Google Sheets)" }
        }
    }
}

/** In-app box to turn the public link on/off. */
fun FlowContent.ShareBox(project: Project, url: String?) = div("share-box") {
    id = "share-box"
    if (url == null) {
        div("grow") {
            b { +"Публічне посилання вимкнене" }
            div("muted small") { +"Увімкніть, щоб актори могли дивитися таблицю без входу" }
        }
        Button("Увімкнути", ButtonVariant.Primary, small = true) {
            hxPost("/tools/sub/${project.id}/share?action=on"); hxTarget("#share-box"); hxSwap("outerHTML")
        }
    } else {
        div("grow") {
            Row(gap = 6) { Badge("Посилання увімкнене", BadgeTone.Success) }
            div("muted small") { +"Будь-хто з посиланням бачить таблицю й картинки (без редагування)" }
        }
        Button("Нове", small = true, variant = ButtonVariant.Tertiary) {
            attributes["title"] = "Згенерувати нове посилання (старе перестане працювати)"
            hxPost("/tools/sub/${project.id}/share?action=regen"); hxTarget("#share-box"); hxSwap("outerHTML")
            hxConfirm("Старе посилання перестане працювати. Продовжити?")
        }
        Button("Вимкнути", small = true, variant = ButtonVariant.Danger) {
            hxPost("/tools/sub/${project.id}/share?action=off"); hxTarget("#share-box"); hxSwap("outerHTML")
        }
    }
}

// ---------------- image management dialog ----------------

fun FlowContent.ImagesDialog(projectId: Int, characters: List<Character>, versions: Map<Int, Long>) =
    Dialog("Зображення персонажів", wide = true) {
        TextField(null, "q", placeholder = "Пошук персонажа…") {
            hxGet("/tools/sub/$projectId/images/rows"); hxTrigger("input changed delay:200ms")
            hxTarget("#image-rows"); attributes["autocomplete"] = "off"
        }
        div("char-scroll char-scroll-tall image-table") {
            table("table") {
                thead { tr { th { +"Ім'я" }; th { +"Прев'ю" }; th { +"" } } }
                tbody { id = "image-rows"; ImageRows(projectId, characters, versions) }
            }
        }
        p("muted small") { +"JPG, PNG, GIF або WebP. Великі картинки автоматично зменшуються." }
    }

fun TBODY.ImageRows(projectId: Int, characters: List<Character>, versions: Map<Int, Long>) {
    if (characters.isEmpty()) tr { td { colSpan = "3"; EmptyState("Нікого не знайдено") } }
    characters.sortedBy { it.name.lowercase() }.forEach { ImageRow(projectId, it, versions[it.id]) }
}

fun TBODY.ImageRow(projectId: Int, c: Character, version: Long?) = tr {
    id = "img-row-${c.id}"
    td { +c.name }
    td {
        if (version != null) img(c.name, "/tools/sub/$projectId/images/${c.id}?v=$version", classes = "thumb")
        else span("thumb thumb-empty") { Icon(Icon.User) }
    }
    td("actions") {
        form(classes = "inline-form") {
            hxPost("/tools/sub/$projectId/images/${c.id}"); hxEncoding("multipart/form-data")
            hxTrigger("change"); hxTarget("#img-row-${c.id}"); hxSwap("outerHTML")
            label("btn btn-secondary btn-small") {
                Icon(Icon.Upload); span { +if (version == null) "Завантажити" else "Замінити" }
                input(InputType.file, name = "image", classes = "visually-hidden") { accept = "image/*" }
            }
        }
        if (version != null) Button(small = true, variant = ButtonVariant.Tertiary, icon = Icon.Trash) {
            attributes["aria-label"] = "Видалити картинку"
            hxDelete("/tools/sub/$projectId/images/${c.id}"); hxTarget("#img-row-${c.id}"); hxSwap("outerHTML")
        }
    }
}

object SheetStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".sheet-head") { display = Display.flex; alignItems = Align.end; gap = 16.px; flexWrap = FlexWrap.wrap; marginBottom = 16.px }
        rule(".sheet-head > div:first-child") { flexGrow = 1.0 }
        rule(".pills") { display = Display.flex; flexWrap = FlexWrap.wrap; gap = 6.px; marginBottom = 16.px }
        rule(".pill") {
            padding = Padding(6.px, 14.px); borderRadius = 999.px; backgroundColor = tc("base"); color = tc("text-secondary")
            fontWeight = FontWeight.w500; put("box-shadow", v("shadow-s")); put("transition", "background-color .15s, color .15s")
        }
        rule(".pill:hover") { textDecoration = TextDecoration.none; color = tc("primary-text") }
        rule(".pill.active") { backgroundColor = tc("primary"); color = Color.white }
        rule(".sheet-card") { marginBottom = 16.px }
        rule(".sheet-group") { display = Display.flex; alignItems = Align.center; gap = 10.px; marginBottom = 8.px }
        rule(".sheet-group h2") { fontSize = 1.15.rem }
        rule(".sheet-name") { fontWeight = FontWeight.w500; width = 40.pct }
        rule(".sheet-img") {
            display = Display.block; maxHeight = 150.px; maxWidth = 220.px; borderRadius = LinearDimension(v("radius-s"))
            put("box-shadow", v("shadow-s")); put("transition", "transform .2s ${v("ease")}")
        }
        rule(".sheet-img-link:hover .sheet-img") { transform { scale(1.03) } }
        rule(".sheet-actions") { display = Display.flex; alignItems = Align.end; justifyContent = JustifyContent.flexEnd; gap = 12.px; flexWrap = FlexWrap.wrap }
        rule(".share-field") { flexGrow = 1.0; maxWidth = 520.px }
        rule(".share-field .link-input") { height = 38.px; fontSize = 0.875.rem; marginTop = 0.px }
        rule(".btn-sheets") { backgroundColor = Color("hsl(145, 63%, 32%)") }
        rule(".btn-sheets:hover") { backgroundColor = Color("hsl(145, 63%, 27%)") }
        rule(".share-box") {
            display = Display.flex; alignItems = Align.center; gap = 10.px; minWidth = 340.px; maxWidth = 520.px
            padding = Padding(10.px, 12.px); backgroundColor = tc("base"); borderRadius = LinearDimension(v("radius-m"))
            put("box-shadow", v("shadow-s")); put("animation", "fade-in .2s ${v("ease")}")
        }

        rule(".thumb") {
            display = Display.flex; width = 44.px; height = 44.px; borderRadius = LinearDimension(v("radius-s"))
            put("object-fit", "cover"); put("box-shadow", v("shadow-s")); put("animation", "pop-in .25s ${v("ease")}")
        }
        rule(".thumb-empty") { alignItems = Align.center; justifyContent = JustifyContent.center; backgroundColor = tc("contrast-5"); color = tc("text-muted"); put("box-shadow", "none") }
        rule(".image-table") { marginTop = 10.px; marginBottom = 8.px }
        rule(".inline-form") { display = Display.inlineBlock; margin = Margin(0.px) }
        rule(".ws-tools") { display = Display.flex; gap = 8.px; flexWrap = FlexWrap.wrap }
    }
}

package com.magnariuk.lemonka.features.calculator

import com.magnariuk.lemonka.data.db.Project
import com.magnariuk.lemonka.subs.CountReport
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*
import java.util.Base64

fun FlowContent.CalculatorPage(projects: List<Project>) {
    div("page-title") {
        h1 { +"Калькулятор слів" }
        p("muted") { +"Порахуйте репліки, слова й числа для обраних персонажів у файлах .srt та .ass" }
    }
    div("calc-grid") {
        Card("Що рахуємо") {
            form {
                hxPost(CalculatorFeature.PATH); hxEncoding("multipart/form-data")
                hxTarget("#calc-result"); hxSwap("innerHTML")
                Column(gap = 16) {
                    Upload("files", ".srt,.ass", "Можна кілька файлів .srt і .ass одночасно")
                    Select("Проєкт", "project", projects.map { Option("${it.id}", it.name) },
                        placeholder = if (projects.isEmpty()) "Проєктів ще немає" else "Оберіть проєкт") {
                        hxGet("${CalculatorFeature.PATH}/characters"); hxTrigger("change"); hxTarget("#char-picker")
                    }
                    Switch("Врахувати всіх персонажів у субтитрах", "all", checked = false)
                    div { id = "char-picker"; CharacterPicker(emptyList(), projectChosen = false) }
                    Button("Порахувати", ButtonVariant.Primary, icon = Icon.Calculator, type = ButtonType.submit)
                }
            }
        }
        div { id = "calc-result"; Card { EmptyState("Тут з'явиться результат", Icon.Calculator) } }
    }
}

fun FlowContent.CharacterPicker(names: List<String>, projectChosen: Boolean) = div("field") {
    span("field-label") { +"Персонажі" }
    when {
        !projectChosen -> div("muted small") { +"Спершу оберіть проєкт" }
        names.isEmpty() -> div("muted small") { +"У проєкті ще немає персонажів" }
        else -> div("char-scroll") { CheckList("character", names.sortedBy { it.lowercase() }.map { Option(it, it) }, emptySet()) }
    }
}

fun FlowContent.CalculatorResult(r: CountReport) = Card("Пораховано") {
    div("stats") {
        Stat("Репліки", r.lines)
        Stat("Слова", r.words)
        Stat("Слова в числах", r.numberWords)
        Stat("Разом", r.total, accent = true)
    }
    p("muted small") {
        +(if (r.characters.isEmpty()) "Усі персонажі" else r.characters.joinToString(" · "))
        +" - числа рекомендується перевірити вручну."
    }
    Table(listOf("Файл", "Репліки", "Слова", "Числа", "Разом")) {
        r.files.forEach { f ->
            tr {
                td { b { +f.file } }
                td { +"${f.lines}" }; td { +"${f.words}" }; td { +"${f.numberWords}" }; td { b { +"${f.total}" } }
            }
        }
    }
    r.files.filter { it.numbers.isNotEmpty() }.forEach { f ->
        details("numbers") {
            summary { +"Числа у ${f.file} (${f.numbers.size})" }
            div("number-list") { f.numbers.forEach { n -> span("chip") { +"${n.number} = ${n.words}" } } }
        }
    }
    Row(gap = 8, classes = "result-actions") {
        val data = Base64.getEncoder().encodeToString(r.toText().toByteArray(Charsets.UTF_8))
        a(href = "data:text/plain;charset=utf-8;base64,$data", classes = "btn btn-primary") {
            hxBoost(false)
            attributes["download"] = "Пораховані субтитри.txt"
            Icon(Icon.Download); span { +"Завантажити .txt" }
        }
    }
}

private fun FlowContent.Stat(label: String, value: Int, accent: Boolean = false) = div("stat${if (accent) " stat-accent" else ""}") {
    div("stat-value") { +"%,d".format(value).replace(',', ' ') }
    div("stat-label") { +label }
}

object CalculatorStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".page-title") { marginBottom = 20.px }
        rule(".page-title p") { margin = Margin(6.px, 0.px, 0.px) }
        rule(".calc-grid") {
            display = Display.grid; gap = 16.px; alignItems = Align.start
            put("grid-template-columns", "minmax(0, 1fr) minmax(0, 1.3fr)")
        }
        media("(max-width: 900px)") { rule(".calc-grid") { put("grid-template-columns", "1fr") } }
        rule(".char-scroll") {
            maxHeight = 240.px; overflowY = Overflow.auto; padding = Padding(4.px)
            put("border", "1px solid ${v("contrast-10")}"); borderRadius = LinearDimension(v("radius-s"))
        }
        rule(".stats") { display = Display.grid; gap = 10.px; put("grid-template-columns", "repeat(4, minmax(0, 1fr))"); marginBottom = 12.px }
        media("(max-width: 560px)") { rule(".stats") { put("grid-template-columns", "repeat(2, minmax(0, 1fr))") } }
        rule(".stat") {
            padding = Padding(14.px); borderRadius = LinearDimension(v("radius-m")); backgroundColor = tc("contrast-5")
            put("animation", "pop-in .3s ${v("ease")} both")
        }
        rule(".stat:nth-child(2)") { put("animation-delay", ".04s") }
        rule(".stat:nth-child(3)") { put("animation-delay", ".08s") }
        rule(".stat:nth-child(4)") { put("animation-delay", ".12s") }
        rule(".stat-value") { fontSize = 1.6.rem; fontWeight = FontWeight.w700; put("font-variant-numeric", "tabular-nums") }
        rule(".stat-label") { fontSize = 0.8125.rem; color = tc("text-secondary") }
        rule(".stat-accent") { backgroundColor = tc("primary"); color = Color.white }
        rule(".stat-accent .stat-label") { color = Color("hsla(0,0%,100%,.85)") }
        rule(".numbers") { marginTop = 12.px }
        rule(".numbers summary") { cursor = Cursor.pointer; color = tc("primary-text"); fontWeight = FontWeight.w500 }
        rule(".number-list") { display = Display.flex; flexWrap = FlexWrap.wrap; gap = 6.px; marginTop = 8.px }
        rule(".result-actions") { marginTop = 16.px }
    }
}

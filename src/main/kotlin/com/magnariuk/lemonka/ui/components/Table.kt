package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** Simple data table (replaces Vaadin's Grid). Rows are written in [rows]. */
fun FlowContent.Table(columns: List<String>, id: String? = null, rows: TBODY.() -> Unit) =
    div("table-wrap") {
        table("table") {
            id?.let { this.id = it }
            thead { tr { columns.forEach { th { +it } } } }
            tbody { rows() }
        }
    }

/** Placeholder shown when a list has nothing in it. */
fun FlowContent.EmptyState(text: String, icon: Icon = Icon.Alert) = div("empty") {
    Icon(icon); span { +text }
}

object TableStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".table-wrap") { overflowX = Overflow.auto }
        rule(".table") { width = 100.pct; put("border-collapse", "collapse") }
        rule(".table th") {
            textAlign = TextAlign.left; fontSize = 0.8125.rem; fontWeight = FontWeight.w500
            color = tc("text-muted"); padding = Padding(8.px, 10.px)
            put("border-bottom", "1px solid ${v("contrast-10")}")
        }
        rule(".table td") { padding = Padding(8.px, 10.px); put("border-bottom", "1px solid ${v("contrast-5")}") }
        rule(".table tbody tr") { put("transition", "background-color .15s") }
        rule(".table tbody tr:hover") { backgroundColor = tc("contrast-5") }
        rule(".table td.actions") { textAlign = TextAlign.right; whiteSpace = WhiteSpace.nowrap }
        rule(".table tr.htmx-swapping") { opacity = 0; put("transition", "opacity .25s") }

        rule(".empty") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center; gap = 8.px
            padding = Padding(24.px); color = tc("text-muted")
        }
    }
}

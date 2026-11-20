package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Breakpoints
import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/**
 * Data table (replaces Vaadin's Grid). Rows are written in [rows] as usual.
 * On phones it turns into a list of cards: every cell is labelled with its column name automatically,
 * so callers don't have to do anything for mobile.
 */
fun FlowContent.Table(columns: List<String>, id: String? = null, rows: TBODY.() -> Unit) =
    div("table-wrap") {
        table("table") {
            id?.let { this.id = it }
            thead { tr { columns.forEach { th { +it } } } }
            TBODY(emptyMap(), CellLabeler(consumer, columns)).visit(rows)
        }
    }

/** Adds `data-label="<column name>"` to each `<td>` as it is written (used by the phone layout). */
private class CellLabeler<T>(private val out: TagConsumer<T>, private val labels: List<String>) : TagConsumer<T> by out {
    private var column = 0
    override fun onTagStart(tag: Tag) {
        if (tag is TR) column = 0
        out.onTagStart(tag)
        if (tag is TD) {
            labels.getOrNull(column)?.takeIf { it.isNotBlank() }?.let { tag.attributes["data-label"] = it }
            column += tag.attributes["colspan"]?.toIntOrNull() ?: 1
        }
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

        // Phones: each row becomes a small card; cells show their column name on the left.
        media(Breakpoints.PHONE) {
            rule(".table thead") { display = Display.none }
            rule(".table, .table tbody, .table tr, .table td") { display = Display.block; width = 100.pct }
            rule(".table tbody tr") {
                padding = Padding(10.px, 4.px); put("border-bottom", "1px solid ${v("contrast-10")}")
            }
            rule(".table tbody tr:hover") { backgroundColor = Color.transparent }
            rule(".table td") {
                display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.spaceBetween
                gap = 12.px; padding = Padding(3.px, 4.px); put("border-bottom", "none"); textAlign = TextAlign.right
            }
            rule(".table td[data-label]::before") {
                put("content", "attr(data-label)"); color = tc("text-muted"); fontSize = 0.8125.rem
                textAlign = TextAlign.left; flexShrink = 0.0
            }
            rule(".table td:not([data-label])") { justifyContent = JustifyContent.flexStart; textAlign = TextAlign.left }
            rule(".table td:first-child") { fontWeight = FontWeight.w600 }
            rule(".table td.actions") { justifyContent = JustifyContent.flexEnd; flexWrap = FlexWrap.wrap; paddingTop = 8.px; gap = 6.px }
        }
        rule(".empty") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center; gap = 8.px
            padding = Padding(24.px); color = tc("text-muted")
        }
    }
}

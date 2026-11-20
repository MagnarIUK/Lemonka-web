package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Breakpoints
import com.magnariuk.lemonka.ui.core.Styled
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.DIV
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.style

enum class Alignment(val css: String) { Start("flex-start"), Center("center"), End("flex-end"), Stretch("stretch") }
enum class Arrangement(val css: String) {
    Start("flex-start"), Center("center"), End("flex-end"), SpaceBetween("space-between")
}

/** Compose-style Column: children stacked vertically. */
fun FlowContent.Column(
    gap: Int = 12,
    align: Alignment = Alignment.Stretch,
    arrange: Arrangement = Arrangement.Start,
    classes: String = "",
    content: DIV.() -> Unit,
) = div("col $classes".trim()) {
    style = "gap:${gap}px;align-items:${align.css};justify-content:${arrange.css}"
    content()
}

/** Compose-style Row: children side by side, wrapping on small screens. */
fun FlowContent.Row(
    gap: Int = 12,
    align: Alignment = Alignment.Center,
    arrange: Arrangement = Arrangement.Start,
    classes: String = "",
    content: DIV.() -> Unit,
) = div("row $classes".trim()) {
    style = "gap:${gap}px;align-items:${align.css};justify-content:${arrange.css}"
    content()
}

/**
 * Side-by-side columns that stack into one on narrow screens.
 * [widths] are relative, like Compose weights: `Columns(1, 2)` = one third + two thirds.
 */
fun FlowContent.Columns(vararg widths: Number = arrayOf(1, 1), gap: Int = 16, content: DIV.() -> Unit) =
    div("columns") {
        style = "--cols:${widths.joinToString(" ") { "minmax(0, ${it}fr)" }};gap:${gap}px"
        content()
    }

/** Pushes the next siblings in a [Row] to the far end. */
fun FlowContent.Spacer() = div("spacer") {}

object LayoutStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".col") { display = Display.flex; flexDirection = FlexDirection.column }
        rule(".row") { display = Display.flex; flexDirection = FlexDirection.row; flexWrap = FlexWrap.wrap }
        rule(".spacer") { flexGrow = 1.0 }
        // Takes the remaining space but may shrink below its content (so an input + button row never wraps).
        rule(".grow") { put("flex", "1 1 0"); minWidth = 0.px }
        rule(".columns") { display = Display.grid; alignItems = Align.start; put("grid-template-columns", "var(--cols)") }
        rule(".columns > *") { minWidth = 0.px }
        media(Breakpoints.NARROW) { rule(".columns") { put("grid-template-columns", "minmax(0, 1fr)") } }
    }
}

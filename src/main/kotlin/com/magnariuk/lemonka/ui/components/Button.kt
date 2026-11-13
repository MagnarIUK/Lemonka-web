package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

enum class ButtonVariant(val css: String) {
    Primary("btn-primary"), Secondary("btn-secondary"), Tertiary("btn-tertiary"), Danger("btn-danger")
}

/**
 * A button. Wire behaviour through [attrs] with HTMX attributes, e.g.
 * `Button("Видалити", ButtonVariant.Danger) { hxPost("/x"); hxTarget("closest tr") }`.
 */
fun FlowOrInteractiveOrPhrasingContent.Button(
    text: String? = null,
    variant: ButtonVariant = ButtonVariant.Secondary,
    icon: Icon? = null,
    type: ButtonType = ButtonType.button,
    small: Boolean = false,
    attrs: BUTTON.() -> Unit = {},
) = button(type = type, classes = "btn ${variant.css}${if (small) " btn-small" else ""}${if (text == null) " btn-icon" else ""}") {
    attrs()
    if (icon != null) Icon(icon)
    if (text != null) span { +text }
    span("btn-spinner") {}
}

/** An `<a>` that looks like a button (for navigation). */
fun FlowOrInteractiveOrPhrasingContent.LinkButton(
    text: String,
    href: String,
    variant: ButtonVariant = ButtonVariant.Secondary,
    icon: Icon? = null,
) = a(href = href, classes = "btn ${variant.css}") {
    if (icon != null) Icon(icon)
    span { +text }
}

object ButtonStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".btn") {
            display = Display.inlineFlex
            alignItems = Align.center
            justifyContent = JustifyContent.center
            gap = 0.5.em
            height = 38.px
            padding = Padding(0.px, 16.px)
            borderRadius = LinearDimension(v("radius-s"))
            border = Border.none
            put("font", "inherit")
            fontWeight = FontWeight.w500
            cursor = Cursor.pointer
            userSelect = UserSelect.none
            whiteSpace = WhiteSpace.nowrap
            textDecoration = TextDecoration.none
            position = Position.relative
            put("transition", "background-color .15s, color .15s, box-shadow .15s, transform .1s ${v("ease")}")
        }
        rule(".btn:hover") { textDecoration = TextDecoration.none }
        rule(".btn:active") { transform { scale(0.97) } }
        rule(".btn:disabled, .btn[aria-disabled=true]") { opacity = 0.5; put("pointer-events", "none") }
        rule(".btn-small") { height = 30.px; padding = Padding(0.px, 10.px); fontSize = 0.875.rem }
        rule(".btn-icon") { width = 38.px; padding = Padding(0.px) }
        rule(".btn-small.btn-icon") { width = 30.px }

        rule(".btn-primary") { backgroundColor = tc("primary"); color = Color.white; put("box-shadow", v("shadow-s")) }
        rule(".btn-primary:hover") { backgroundColor = tc("primary-hover") }
        rule(".btn-secondary") { backgroundColor = tc("contrast-5"); color = tc("primary-text") }
        rule(".btn-secondary:hover") { backgroundColor = tc("contrast-10") }
        rule(".btn-tertiary") { backgroundColor = Color.transparent; color = tc("primary-text") }
        rule(".btn-tertiary:hover") { backgroundColor = tc("primary-10") }
        rule(".btn-danger") { backgroundColor = tc("error-10"); color = tc("error") }
        rule(".btn-danger:hover") { backgroundColor = tc("error"); color = Color.white }

        // Loading spinner, shown automatically while an HTMX request from this button is in flight
        rule(".btn-spinner") {
            display = Display.none
            width = 14.px; height = 14.px
            borderRadius = 50.pct
            put("border", "2px solid currentColor")
            put("border-right-color", "transparent")
            put("animation", "spin .7s linear infinite")
        }
        rule(".btn.htmx-request .btn-spinner") { display = Display.inlineBlock }
        rule(".btn.htmx-request") { put("pointer-events", "none"); opacity = 0.85 }
    }
}

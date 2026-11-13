package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** Labelled text input with an optional error message under it. */
fun FlowContent.TextField(
    label: String?,
    name: String,
    value: String = "",
    type: InputType = InputType.text,
    placeholder: String? = null,
    error: String? = null,
    required: Boolean = false,
    autofocus: Boolean = false,
    attrs: INPUT.() -> Unit = {},
) = label("field${if (error != null) " field-invalid" else ""}") {
    if (label != null) span("field-label") {
        +label
        if (required) span("field-required") { +" •" }
    }
    input(type = type, name = name, classes = "input") {
        this.value = value
        placeholder?.let { this.placeholder = it }
        this.required = required
        this.autoFocus = autofocus
        if (error != null) attributes["aria-invalid"] = "true"
        attrs()
    }
    if (error != null) span("field-error") { Icon(Icon.Alert); +error }
}

object TextFieldStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".field") { display = Display.flex; flexDirection = FlexDirection.column; gap = 4.px; minWidth = 0.px }
        rule(".field-label") { fontSize = 0.875.rem; fontWeight = FontWeight.w500; color = tc("text-secondary") }
        rule(".field-required") { color = tc("primary") }
        rule(".input, .select") {
            height = 38.px
            padding = Padding(0.px, 12.px)
            put("font", "inherit")
            color = tc("text")
            backgroundColor = tc("contrast-5")
            border = Border(1.px, BorderStyle.solid, Color.transparent)
            borderRadius = LinearDimension(v("radius-s"))
            put("transition", "background-color .15s, border-color .15s, box-shadow .15s")
            width = 100.pct
        }
        rule(".input:hover, .select:hover") { backgroundColor = tc("contrast-10") }
        rule(".input:focus, .select:focus") {
            put("outline", "none")
            backgroundColor = tc("base")
            borderColor = tc("primary")
            put("box-shadow", "0 0 0 3px ${v("primary-30")}")
        }
        rule(".field-invalid .input") { backgroundColor = tc("error-10"); borderColor = tc("error") }
        rule(".field-error") {
            display = Display.flex; alignItems = Align.center; gap = 4.px
            fontSize = 0.8125.rem; color = tc("error")
            put("animation", "fade-in .2s ${v("ease")}")
        }
    }
}

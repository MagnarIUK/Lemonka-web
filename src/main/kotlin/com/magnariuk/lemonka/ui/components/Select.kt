package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

data class Option(val value: String, val label: String)

/** Native dropdown, styled like the text field. */
fun FlowContent.Select(
    label: String?,
    name: String,
    options: List<Option>,
    selected: String? = null,
    placeholder: String? = null,
    required: Boolean = false,
    attrs: SELECT.() -> Unit = {},
) = label("field") {
    if (label != null) span("field-label") {
        +label
        if (required) span("field-required") { +" •" }
    }
    select(classes = "select") {
        this.name = name
        this.required = required
        attrs()
        if (placeholder != null) option {
            value = ""; disabled = true; this.selected = selected == null; +placeholder
        }
        options.forEach { o -> option { value = o.value; this.selected = o.value == selected; +o.label } }
    }
}

/** Checkbox drawn as a toggle switch. */
fun FlowContent.Switch(label: String, name: String, checked: Boolean, attrs: INPUT.() -> Unit = {}) =
    label("switch") {
        input(type = InputType.checkBox, name = name) { this.checked = checked; value = "true"; attrs() }
        span("switch-track") { span("switch-thumb") {} }
        span { +label }
    }

/** A list of checkboxes (replaces Vaadin's MultiSelectComboBox). */
fun FlowContent.CheckList(name: String, options: List<Option>, checked: Set<String>, attrs: INPUT.() -> Unit = {}) =
    div("checklist") {
        options.forEach { o ->
            label("check") {
                input(type = InputType.checkBox, name = name) {
                    value = o.value; this.checked = o.value in checked; attrs()
                }
                span { +o.label }
            }
        }
    }

object SelectStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".select") {
            put("appearance", "none")
            paddingRight = 34.px
            cursor = Cursor.pointer
            put("background-image", "linear-gradient(45deg, transparent 50%, currentColor 50%), linear-gradient(135deg, currentColor 50%, transparent 50%)")
            put("background-position", "calc(100% - 18px) 16px, calc(100% - 13px) 16px")
            put("background-size", "5px 5px")
            put("background-repeat", "no-repeat")
        }

        rule(".switch") { display = Display.inlineFlex; alignItems = Align.center; gap = 10.px; cursor = Cursor.pointer }
        rule(".switch input") { position = Position.absolute; opacity = 0; put("pointer-events", "none") }
        rule(".switch-track") {
            width = 36.px; height = 20.px; borderRadius = 10.px
            backgroundColor = tc("contrast-20")
            position = Position.relative
            put("transition", "background-color .2s ${v("ease")}")
        }
        rule(".switch-thumb") {
            position = Position.absolute; top = 2.px; left = 2.px
            width = 16.px; height = 16.px; borderRadius = 50.pct
            backgroundColor = Color.white
            put("box-shadow", v("shadow-s"))
            put("transition", "transform .2s ${v("ease")}")
        }
        rule(".switch input:checked + .switch-track") { backgroundColor = tc("primary") }
        rule(".switch input:checked + .switch-track .switch-thumb") { transform { translateX(16.px) } }
        rule(".switch input:focus-visible + .switch-track") { put("box-shadow", "0 0 0 3px ${v("primary-30")}") }

        rule(".checklist") { display = Display.flex; flexDirection = FlexDirection.column; gap = 2.px }
        rule(".check") {
            display = Display.flex; alignItems = Align.center; gap = 8.px
            padding = Padding(4.px, 8.px); borderRadius = LinearDimension(v("radius-s")); cursor = Cursor.pointer
        }
        rule(".check:hover") { backgroundColor = tc("contrast-5") }
        rule(".check input") { put("accent-color", v("primary")); width = 16.px; height = 16.px }
    }
}

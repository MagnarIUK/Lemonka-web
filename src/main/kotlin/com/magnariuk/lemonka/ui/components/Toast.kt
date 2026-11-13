package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

enum class ToastTone(val css: String, val icon: Icon) {
    Success("toast-success", Icon.Check), Error("toast-error", Icon.Alert), Info("toast-info", Icon.Alert)
}

/**
 * A notification (replaces Vaadin's Notification / showError / showSuccess).
 * Add it to any HTMX response; it is moved into the corner via an out-of-band swap and fades out on its own.
 */
fun FlowContent.Toast(message: String, tone: ToastTone = ToastTone.Success) = div {
    hxSwapOob("beforeend:#toasts")
    div("toast ${tone.css}") {
        attributes["role"] = "status"
        Icon(tone.icon)
        span { +message }
    }
}

object ToastStyle : Styled {
    override fun CssBuilder.styles() {
        rule("#toasts") {
            position = Position.fixed; right = 16.px; bottom = 16.px; zIndex = 100
            display = Display.flex; flexDirection = FlexDirection.column; gap = 8.px
            put("pointer-events", "none")
        }
        rule(".toast") {
            display = Display.flex; alignItems = Align.center; gap = 10.px
            padding = Padding(12.px, 16.px); minWidth = 240.px; maxWidth = 380.px
            borderRadius = LinearDimension(v("radius-m"))
            backgroundColor = tc("base")
            put("box-shadow", v("shadow-l"))
            put("border-left", "4px solid currentColor")
            put("animation", "slide-in .25s ${v("ease")}, fade-out .4s ease 4s forwards")
        }
        rule(".toast span") { color = tc("text") }
        rule(".toast-success") { color = tc("success") }
        rule(".toast-error") { color = tc("error") }
        rule(".toast-info") { color = tc("primary") }
    }
}

package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** Endpoint that returns nothing; used by close buttons together with `hx-swap="delete"`. */
const val NOOP_URL = "/ui/noop"

private fun HTMLTag.closesModal() {
    hxGet(NOOP_URL); hxTarget("closest .modal"); hxSwap("delete")
}

/**
 * Modal dialog. Return it from a route as a fragment and target `#modal`:
 * `Button("Новий") { hxGet("/projects/new"); hxTarget("#modal") }`.
 * Closes on the × button, backdrop click or Escape - all through HTMX, no custom JS.
 */
fun FlowContent.Dialog(
    title: String,
    footer: (DIV.() -> Unit)? = null,
    wide: Boolean = false,
    content: DIV.() -> Unit,
) = div("modal") {
    div("modal-backdrop") { closesModal() }
    div("modal-panel${if (wide) " modal-wide" else ""}") {
        attributes["role"] = "dialog"
        attributes["aria-modal"] = "true"
        div("modal-header") {
            h2 { +title }
            Button(variant = ButtonVariant.Tertiary, icon = Icon.Close, small = true) {
                attributes["aria-label"] = "Закрити"
                closesModal()
                hxTrigger("click, keyup[key=='Escape'] from:body")
            }
        }
        div("modal-body") { content() }
        if (footer != null) div("modal-footer") { footer() }
    }
}

/** A dialog's own close button (for "Скасувати"). */
fun FlowOrInteractiveOrPhrasingContent.DialogCloseButton(text: String = "Скасувати") =
    Button(text, ButtonVariant.Tertiary) { closesModal() }

object DialogStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".modal") {
            position = Position.fixed; put("inset", "0"); zIndex = 50
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center
            padding = Padding(16.px)
        }
        rule(".modal-backdrop") {
            position = Position.absolute; put("inset", "0")
            backgroundColor = Color("hsla(214, 50%, 10%, 0.4)")
            put("backdrop-filter", "blur(2px)")
            put("animation", "fade-in .2s ${v("ease")}")
        }
        rule(".modal-panel") {
            position = Position.relative
            width = 100.pct; maxWidth = 480.px; maxHeight = 90.vh
            display = Display.flex; flexDirection = FlexDirection.column
            backgroundColor = tc("base")
            borderRadius = LinearDimension(v("radius-l"))
            put("box-shadow", v("shadow-l"))
            put("animation", "pop-in .22s ${v("ease")}")
        }
        rule(".modal-wide") { maxWidth = 860.px }
        rule(".modal-header") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.spaceBetween
            padding = Padding(18.px, 20.px, 8.px, 20.px)
        }
        rule(".modal-body") { padding = Padding(8.px, 20.px, 20.px); overflowY = Overflow.auto }
        rule(".modal-footer") {
            display = Display.flex; justifyContent = JustifyContent.flexEnd; gap = 8.px
            padding = Padding(12.px, 20.px)
            put("border-top", "1px solid ${v("contrast-10")}")
        }
    }
}

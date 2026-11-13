package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** White surface with a soft shadow. Optional title row with trailing actions. */
fun FlowContent.Card(
    title: String? = null,
    classes: String = "",
    id: String? = null,
    attrs: DIV.() -> Unit = {},
    actions: (DIV.() -> Unit)? = null,
    content: DIV.() -> Unit,
) = div("card $classes".trim()) {
    id?.let { this.id = it }
    attrs() // attributes must be set before any child is written
    if (title != null || actions != null) div("card-header") {
        if (title != null) h3 { +title }
        if (actions != null) div("card-actions") { actions() }
    }
    content()
}

/** A clickable card that links somewhere (used for tool tiles on the home page). */
fun FlowContent.TileLink(href: String, icon: Icon, title: String, description: String, badge: String? = null) =
    a(href = href, classes = "card tile") {
        span("tile-icon") { Icon(icon) }
        div {
            div("tile-title") { +title; if (badge != null) Badge(badge, BadgeTone.Neutral) }
            div("muted") { +description }
        }
    }

enum class BadgeTone(val css: String) { Primary("badge-primary"), Neutral("badge-neutral"), Success("badge-success"), Danger("badge-danger") }

fun FlowOrPhrasingContent.Badge(text: String, tone: BadgeTone = BadgeTone.Primary) =
    span("badge ${tone.css}") { +text }

object CardStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".card") {
            backgroundColor = tc("base")
            borderRadius = LinearDimension(v("radius-m"))
            put("box-shadow", v("shadow-s"))
            padding = Padding(20.px)
            put("animation", "fade-in .3s ${v("ease")} both")
        }
        rule(".card-header") {
            display = Display.flex; alignItems = Align.center; gap = 12.px
            justifyContent = JustifyContent.spaceBetween
            marginBottom = 14.px
        }
        rule(".card-actions") { display = Display.flex; gap = 8.px }

        rule(".tile") {
            display = Display.flex; alignItems = Align.center; gap = 16.px
            color = tc("text")
            put("transition", "transform .2s ${v("ease")}, box-shadow .2s ${v("ease")}")
        }
        rule(".tile:hover") { textDecoration = TextDecoration.none; transform { translateY((-2).px) }; put("box-shadow", v("shadow-m")) }
        rule(".tile-icon") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center
            width = 48.px; height = 48.px; flexShrink = 0.0
            borderRadius = LinearDimension(v("radius-m"))
            backgroundColor = tc("primary-10"); color = tc("primary")
            fontSize = 22.px
        }
        rule(".tile-title") { fontWeight = FontWeight.w600; fontSize = 1.05.rem; display = Display.flex; flexWrap = FlexWrap.wrap; gap = LinearDimension("4px 8px"); alignItems = Align.center }
        rule(".tile > div") { flexGrow = 1.0; minWidth = 0.px }

        rule(".badge") {
            display = Display.inlineFlex; alignItems = Align.center
            padding = Padding(1.px, 8.px); borderRadius = 999.px
            fontSize = 0.75.rem; fontWeight = FontWeight.w500
        }
        rule(".badge-primary") { backgroundColor = tc("primary-10"); color = tc("primary-text") }
        rule(".badge-neutral") { backgroundColor = tc("contrast-10"); color = tc("text-secondary") }
        rule(".badge-success") { backgroundColor = tc("success-10"); color = tc("success") }
        rule(".badge-danger") { backgroundColor = tc("error-10"); color = tc("error") }
    }
}

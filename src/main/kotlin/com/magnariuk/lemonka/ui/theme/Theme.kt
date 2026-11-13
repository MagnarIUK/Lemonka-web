package com.magnariuk.lemonka.ui.theme

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*

/** Design tokens. Blue/white palette modelled on Vaadin's Lumo theme. */
object Theme : Styled {
    private val tokens = mapOf(
        "primary" to "hsl(214, 100%, 48%)",
        "primary-hover" to "hsl(214, 100%, 42%)",
        "primary-text" to "hsl(214, 100%, 43%)",
        "primary-10" to "hsla(214, 100%, 60%, 0.13)",
        "primary-30" to "hsla(214, 100%, 60%, 0.3)",
        "base" to "#ffffff",
        "surface" to "hsl(214, 35%, 97%)",
        "text" to "hsla(214, 40%, 16%, 0.94)",
        "text-secondary" to "hsla(214, 42%, 18%, 0.69)",
        "text-muted" to "hsla(214, 45%, 20%, 0.52)",
        "contrast-5" to "hsla(214, 61%, 25%, 0.05)",
        "contrast-10" to "hsla(214, 57%, 24%, 0.1)",
        "contrast-20" to "hsla(214, 53%, 23%, 0.16)",
        "error" to "hsl(3, 85%, 48%)",
        "error-10" to "hsla(3, 85%, 49%, 0.1)",
        "success" to "hsl(145, 72%, 30%)",
        "success-10" to "hsla(145, 72%, 31%, 0.1)",
        "radius-s" to "6px",
        "radius-m" to "10px",
        "radius-l" to "16px",
        "shadow-s" to "0 1px 2px hsla(214, 60%, 20%, 0.08), 0 2px 6px hsla(214, 60%, 20%, 0.06)",
        "shadow-m" to "0 4px 12px hsla(214, 60%, 20%, 0.1), 0 2px 4px hsla(214, 60%, 20%, 0.06)",
        "shadow-l" to "0 18px 48px hsla(214, 60%, 20%, 0.18)",
        "ease" to "cubic-bezier(.2, .8, .2, 1)",
        "font" to "system-ui, -apple-system, 'Segoe UI', Roboto, Ubuntu, 'Helvetica Neue', sans-serif",
    )

    override fun CssBuilder.styles() {
        rule(":root") { tokens.forEach { (k, value) -> put("--$k", value) } }

        rule("*, *::before, *::after") { boxSizing = BoxSizing.borderBox }
        rule("html, body") { margin = Margin(0.px); padding = Padding(0.px) }
        body {
            put("font-family", v("font"))
            fontSize = 15.px
            lineHeight = LineHeight("1.5")
            color = tc("text")
            backgroundColor = tc("surface")
            minHeight = 100.vh
            put("-webkit-font-smoothing", "antialiased")
        }
        rule("a") { color = tc("primary-text"); textDecoration = TextDecoration.none }
        rule("a:hover") { textDecoration = TextDecoration(setOf(TextDecorationLine.underline)) }
        rule("h1, h2, h3") { margin = Margin(0.px); lineHeight = LineHeight("1.25"); fontWeight = FontWeight.w600 }
        rule("h1") { fontSize = 1.75.rem }
        rule("h2") { fontSize = 1.35.rem }
        rule("h3") { fontSize = 1.1.rem }
        rule(":focus-visible") { put("outline", "2px solid ${v("primary")}"); put("outline-offset", "2px") }
        rule(".muted") { color = tc("text-muted") }

        // Shared animations used by several components
        rule("@keyframes fade-in") {
            rule("from") { opacity = 0; transform { translateY(6.px) } }
            rule("to") { opacity = 1; transform { translateY(0.px) } }
        }
        rule("@keyframes pop-in") {
            rule("from") { opacity = 0; transform { scale(0.96) } }
            rule("to") { opacity = 1; transform { scale(1) } }
        }
        rule("@keyframes slide-in") {
            rule("from") { opacity = 0; transform { translateX(24.px) } }
            rule("to") { opacity = 1; transform { translateX(0.px) } }
        }
        rule("@keyframes fade-out") { rule("to") { opacity = 0; visibility = Visibility.hidden } }
        rule("@keyframes spin") { rule("to") { transform { rotate(360.deg) } } }

        // HTMX: elements swapped in get a soft entrance
        rule(".htmx-added") { opacity = 0 }
        rule(".htmx-settling") { put("transition", "opacity .2s ${v("ease")}") }

        media("(prefers-reduced-motion: reduce)") {
            rule("*, *::before, *::after") {
                put("animation-duration", "0.01ms !important")
                put("transition-duration", "0.01ms !important")
            }
        }
    }
}

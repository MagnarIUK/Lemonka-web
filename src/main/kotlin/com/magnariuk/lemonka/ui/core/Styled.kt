package com.magnariuk.lemonka.ui.core

import kotlinx.css.CssBuilder
import kotlinx.css.Color

/**
 * Every UI component is a pair, kept in the same file:
 *  - an `object XxxStyle : Styled` holding the component's CSS, and
 *  - a `fun FlowContent.Xxx(...)` that renders it (the "composable").
 *
 * Styles are collected into one stylesheet by [com.magnariuk.lemonka.ui.theme.Stylesheet].
 * To add a component: create the file, then add its style object to the registry there.
 */
interface Styled {
    fun CssBuilder.styles()
}

/** Shorthand for a theme CSS variable, e.g. `v("primary")` → `var(--primary)`. */
fun v(name: String) = "var(--$name)"

/** A theme color as a typed kotlin-css [Color]. */
fun tc(name: String) = Color(v(name))

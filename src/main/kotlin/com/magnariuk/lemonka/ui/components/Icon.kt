package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.span
import kotlinx.html.unsafe

/** Simple 24×24 stroke icons, drawn inline as SVG. */
enum class Icon(internal val paths: String) {
    Home("<path d='M3 11l9-7 9 7'/><path d='M5 10v10h14V10'/>"),
    User("<circle cx='12' cy='8' r='4'/><path d='M4 21c0-4 4-6 8-6s8 2 8 6'/>"),
    Logout("<path d='M15 4h4v16h-4'/><path d='M10 8l-4 4 4 4'/><path d='M6 12h11'/>"),
    Shield("<path d='M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z'/>"),
    Plus("<path d='M12 5v14M5 12h14'/>"),
    Trash("<path d='M4 7h16'/><path d='M9 7V4h6v3'/><path d='M6 7l1 13h10l1-13'/>"),
    Download("<path d='M12 4v11'/><path d='M7 10l5 5 5-5'/><path d='M5 20h14'/>"),
    Upload("<path d='M12 20V9'/><path d='M7 14l5-5 5 5'/><path d='M5 4h14'/>"),
    Edit("<path d='M4 20h4L19 9l-4-4L4 16z'/>"),
    Check("<path d='M5 12l5 5 9-10'/>"),
    Close("<path d='M6 6l12 12M18 6L6 18'/>"),
    Alert("<circle cx='12' cy='12' r='9'/><path d='M12 7v6M12 16.5v.5'/>"),
    Subtitles("<rect x='3' y='5' width='18' height='14' rx='2'/><path d='M7 13h4M13 13h4M7 16h8'/>"),
    Calculator("<rect x='5' y='3' width='14' height='18' rx='2'/><path d='M8 7h8M8 12h.01M12 12h.01M16 12h.01M8 16h.01M12 16h.01M16 16h.01'/>"),
    ChevronDown("<path d='M6 9l6 6 6-6'/>"),
}

fun FlowOrPhrasingContent.Icon(icon: Icon, classes: String = "") = span("icon $classes".trim()) {
    unsafe {
        +"<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'>${icon.paths}</svg>"
    }
}

object IconStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".icon") {
            display = Display.inlineFlex
            width = 1.15.em; height = 1.15.em
            flexShrink = 0.0
        }
        rule(".icon svg") { width = 100.pct; height = 100.pct }
    }
}

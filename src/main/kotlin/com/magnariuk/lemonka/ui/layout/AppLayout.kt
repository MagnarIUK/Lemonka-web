package com.magnariuk.lemonka.ui.layout

import com.magnariuk.lemonka.AppInfo
import com.magnariuk.lemonka.data.db.User
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** A link in the top navigation bar. Features declare these; see [com.magnariuk.lemonka.web.Feature]. */
data class NavItem(val title: String, val href: String, val icon: Icon)

/** A link in the profile dropdown. `visible` decides per user (e.g. admin-only entries). */
data class MenuItem(val title: String, val href: String, val icon: Icon, val visible: (User) -> Boolean = { true })

/** Everything the shell needs to know about the current request. */
data class Shell(val user: User?, val path: String, val nav: List<NavItem>, val menu: List<MenuItem>)

/** Full HTML page: head, top bar, animated content area, modal and toast slots. */
fun HTML.AppLayout(title: String, shell: Shell, content: MAIN.() -> Unit) {
    lang = "uk"
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        meta(name = "htmx-config", content = """{"globalViewTransitions":true,"scrollIntoViewOnBoost":false}""")
        title { +"$title · ${AppInfo.NAME}" }
        link(rel = "icon", href = "/static/favicon.svg", type = "image/svg+xml")
        link(rel = "stylesheet", href = "/static/app.css?v=${AppInfo.VERSION}")
        script(src = "/static/js/htmx.min.js") { defer = true }
        script(src = "/static/js/dnd.js?v=${AppInfo.VERSION}") { defer = true }
    }
    body {
        hxBoost()
        TopBar(shell)
        main("container page") { content() }
        div { id = "modal" }
        div { id = "toasts" }
    }
}

private fun FlowContent.TopBar(shell: Shell) = header("topbar") {
    div("container topbar-inner") {
        div("brand") {
            a(href = "https://t.me/KokojamboLVP", target = "_blank", classes = "brand-name") {
                hxBoost(false)
                +AppInfo.NAME
            }
            Badge(AppInfo.VERSION, BadgeTone.Neutral)
            span("brand-by muted") {
                +"Made by "
                a(href = "https://github.com/MagnarIUK", target = "_blank") { hxBoost(false); +"magnariuk" }
            }
        }
        val user = shell.user ?: return@div
        nav("nav") {
            shell.nav.forEach { item ->
                val active = if (item.href == "/") shell.path == "/" else shell.path.startsWith(item.href)
                a(href = item.href, classes = "nav-link${if (active) " active" else ""}") {
                    Icon(item.icon); span("nav-text") { +item.title }
                }
            }
        }
        UserMenu(user, shell.menu)
    }
}

/** Dropdown that opens on click/focus using only CSS (:focus-within). */
private fun FlowContent.UserMenu(user: User, items: List<MenuItem>) = div("menu") {
    button(type = ButtonType.button, classes = "menu-trigger") {
        span("avatar") { +user.username.take(1).uppercase() }
        span("nav-text") { +user.username }
        Icon(Icon.ChevronDown)
    }
    div("menu-list") {
        items.filter { it.visible(user) }.forEach { item ->
            a(href = item.href, classes = "menu-item") { Icon(item.icon); span { +item.title } }
        }
        form(action = "/auth/logout", method = FormMethod.post) {
            hxPost("/auth/logout")
            button(type = ButtonType.submit, classes = "menu-item menu-danger") { Icon(Icon.Logout); span { +"Вихід" } }
        }
    }
}

object AppLayoutStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".container") { width = 100.pct; maxWidth = 1160.px; margin = Margin(0.px, LinearDimension.auto); padding = Padding(0.px, 20.px) }
        rule(".page") { padding = Padding(28.px, 20.px, 64.px); put("animation", "fade-in .3s ${v("ease")}") }
        rule("::view-transition-old(root), ::view-transition-new(root)") { put("animation-duration", ".18s") }

        rule(".topbar") {
            position = Position.sticky; top = 0.px; zIndex = 20
            backgroundColor = Color("hsla(0, 0%, 100%, 0.85)")
            put("backdrop-filter", "saturate(1.4) blur(10px)")
            put("box-shadow", "0 1px 0 ${v("contrast-10")}")
        }
        rule(".topbar-inner") { display = Display.flex; alignItems = Align.center; gap = 20.px; height = 60.px }
        rule(".brand") { display = Display.flex; alignItems = Align.center; gap = 8.px; whiteSpace = WhiteSpace.nowrap }
        rule(".brand-name") { fontSize = 1.3.rem; fontWeight = FontWeight.w700; color = tc("primary") }
        rule(".brand-name:hover") { textDecoration = TextDecoration.none }
        rule(".brand-by") { fontSize = 0.75.rem }

        rule(".nav") { display = Display.flex; gap = 4.px; flexGrow = 1.0 }
        rule(".nav-link") {
            display = Display.flex; alignItems = Align.center; gap = 6.px
            padding = Padding(6.px, 12.px); borderRadius = LinearDimension(v("radius-s"))
            color = tc("text-secondary"); fontWeight = FontWeight.w500
            put("transition", "background-color .15s, color .15s")
        }
        rule(".nav-link:hover") { textDecoration = TextDecoration.none; backgroundColor = tc("contrast-5"); color = tc("text") }
        rule(".nav-link.active") { backgroundColor = tc("primary-10"); color = tc("primary-text") }

        rule(".menu") { position = Position.relative; marginLeft = LinearDimension.auto }
        rule(".menu-trigger") {
            display = Display.flex; alignItems = Align.center; gap = 8.px
            padding = Padding(4.px, 8.px, 4.px, 4.px); border = Border.none; backgroundColor = Color.transparent
            borderRadius = 999.px; cursor = Cursor.pointer; put("font", "inherit"); color = tc("text")
        }
        rule(".menu-trigger:hover") { backgroundColor = tc("contrast-5") }
        rule(".avatar") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center
            width = 30.px; height = 30.px; borderRadius = 50.pct
            backgroundColor = tc("primary"); color = Color.white; fontWeight = FontWeight.w600; fontSize = 0.875.rem
        }
        rule(".menu-list") {
            position = Position.absolute; right = 0.px; top = LinearDimension("calc(100% + 6px)")
            minWidth = 200.px; padding = Padding(6.px)
            backgroundColor = tc("base"); borderRadius = LinearDimension(v("radius-m")); put("box-shadow", v("shadow-l"))
            opacity = 0; visibility = Visibility.hidden
            transform { translateY((-4).px) }
            put("transition", "opacity .15s, transform .15s ${v("ease")}, visibility .15s")
        }
        rule(".menu:focus-within .menu-list") { opacity = 1; visibility = Visibility.visible; transform { translateY(0.px) } }
        rule(".menu-item") {
            display = Display.flex; alignItems = Align.center; gap = 10.px; width = 100.pct
            padding = Padding(8.px, 10.px); borderRadius = LinearDimension(v("radius-s"))
            border = Border.none; backgroundColor = Color.transparent; put("font", "inherit")
            color = tc("text"); cursor = Cursor.pointer; textAlign = TextAlign.left
        }
        rule(".menu-item:hover") { backgroundColor = tc("contrast-5"); textDecoration = TextDecoration.none }
        rule(".menu-danger") { color = tc("error") }
        rule(".menu form") { margin = Margin(0.px) }

        media("(max-width: 720px)") {
            rule(".nav-text, .brand-by") { display = Display.none }
            rule(".topbar-inner") { gap = 10.px }
        }
    }
}

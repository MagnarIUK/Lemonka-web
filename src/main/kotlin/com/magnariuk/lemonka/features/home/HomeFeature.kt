package com.magnariuk.lemonka.features.home

import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.features.calculator.CalculatorFeature
import com.magnariuk.lemonka.features.subtitles.SubtitlesFeature
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.layout.NavItem
import com.magnariuk.lemonka.web.Feature
import com.magnariuk.lemonka.web.respondPage
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** One tile on the home page. Tool features register theirs in [HomeFeature.tools]. */
data class ToolTile(val href: String, val icon: Icon, val title: String, val description: String, val ready: Boolean = true)

object HomeFeature : Feature {
    override val nav = NavItem("Головна", "/", Icon.Home)

    // A tool still in progress can be shown with ready = false (gets a "незабаром" badge).
    val tools = listOf(
        ToolTile(SubtitlesFeature.PATH, Icon.Subtitles, "Розділення субтитрів",
            "Розділіть .ass на окремі файли для кожного актора"),
        ToolTile(CalculatorFeature.PATH, Icon.Calculator, "Калькулятор слів",
            "Порахуйте слова, репліки й числа для персонажів"),
    )

    override fun Route.routes() {
        get("/") {
            call.respondPage("Головна") {
                div("home-hero") {
                    h1 { +"Привіт, ${call.user.username}!" }
                    p("muted") { +"Оберіть інструмент, з яким хочете працювати." }
                }
                div("home-grid") {
                    tools.forEach { t ->
                        TileLink(t.href, t.icon, t.title, t.description, badge = if (t.ready) null else "незабаром")
                    }
                }
            }
        }
        get("/tools") { call.respondRedirect("/") } // old Vaadin home URL
    }
}

object HomeStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".home-hero") { marginBottom = 24.px }
        rule(".home-hero p") { margin = Margin(6.px, 0.px, 0.px) }
        rule(".home-grid") {
            display = Display.grid; gap = 16.px
            put("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
        }
        rule(".home-grid .tile:nth-child(2)") { put("animation-delay", ".05s") }
        rule(".home-grid .tile:nth-child(3)") { put("animation-delay", ".1s") }
    }
}

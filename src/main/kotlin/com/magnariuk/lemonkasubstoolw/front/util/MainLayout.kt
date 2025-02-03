package com.magnariuk.lemonkasubstoolw.front.util

import com.magnariuk.lemonkasubstoolw.data.api.database.AuthService
import com.magnariuk.lemonkasubstoolw.data.util.*
import com.magnariuk.lemonkasubstoolw.front.views.tools.ActorMeter
import com.magnariuk.lemonkasubstoolw.front.views.tools.MainToolsView
import com.magnariuk.lemonkasubstoolw.front.views.tools.SubsToolView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.contextmenu.MenuItem
import com.vaadin.flow.component.contextmenu.SubMenu
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.menubar.MenuBar
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import org.springframework.beans.factory.annotation.Autowired

const val VERSION = "0.1.1b"

class MainLayout(
    @Autowired private val authService: AuthService
): AppLayout() {
    init {

        val lamonka = Anchor("https://t.me/KokojamboLVP", "Lem0nka Tools $VERSION").apply {
            setTarget("_blank")
            style.set(CSS.FONT_SIZE, 25.px)
            style.set(CSS.OUTLINE, "none")
        }
        val magnar = Anchor("https://github.com/MagnarIUK", "magnariuk").apply {
            setTarget("_blank")
            style.set(CSS.FONT_SIZE, 10.px)
            style.set(CSS.OUTLINE, "none")
        }

        val made_By = Paragraph(NativeLabel("Made by ").apply {
            style.set(CSS.FONT_SIZE, 10.px)
            style.set(CSS.OUTLINE, "none")
        }, magnar).apply {  }
        val title = HorizontalLayout(lamonka, made_By)


        val menu = MenuBar().apply {
            val listener: ComponentEventListener<ClickEvent<MenuItem>> =
                ComponentEventListener<ClickEvent<MenuItem>> { e: ClickEvent<MenuItem> ->
                    val item = e.source.text
                    when (item) {
                        "Головна" -> {
                            UI.getCurrent().navigate(MainToolsView::class.java)
                        }
                        "Профіль" -> {
                            val dialog = Dialog().apply{

                            }
                            dialog.open()
                        }
                        "Розділення субтитрів" -> {
                            UI.getCurrent().navigate(SubsToolView::class.java)
                        }
                        "Калькулятор слів" -> {
                            UI.getCurrent().navigate(ActorMeter::class.java)
                        }
                        "Вихід" -> {
                            authService.logout()
                            UI.getCurrent().navigate(MainToolsView::class.java)
                        }
                    }
                }

            val main = addItem("Головна", listener)
            /*val subMainMenu = main.subMenu.apply {
                addItem("Головна")
                addItem("Розділення субтитрів")
                addItem("Калькулятор слів")
            }*/
            val profile = addItem("Профіль")
            val subMenuProfile = profile.subMenu.apply {
                addItem("Профіль", listener)
                addItem("Вихід", listener)
            }
        }
        /* HorizontalLayout(
            Button("Головна").apply {
                addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
                onLeftClick {
                    UI.getCurrent().navigate(MainToolsView::class.java)
                }
            },
        ).apply {
            isSpacing = true
            alignItems = FlexComponent.Alignment.CENTER
        }*/

        val titleWrapper = HorizontalLayout().apply {
            add(title)
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            expand(title)
            setWidthFull()
        }
        val header = HorizontalLayout().apply {
            add(titleWrapper, menu)
            setWidthFull()
            width = 100.p
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            style.set(CSS.MARGIN_LEFT, 30.p).set(CSS.MARGIN_RIGHT, 30.p)
        }

        addToNavbar(header)
    }
}
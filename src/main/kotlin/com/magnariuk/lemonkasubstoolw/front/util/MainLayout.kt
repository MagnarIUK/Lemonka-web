package com.magnariuk.lemonkasubstoolw.front.util

import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.text
import com.magnariuk.lemonkasubstoolw.data.util.CSS
import com.magnariuk.lemonkasubstoolw.data.util.*
import com.magnariuk.lemonkasubstoolw.front.views.tools.MainToolsView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

class MainLayout: AppLayout() {
    init {

        val lamonka = Anchor("https://t.me/KokojamboLVP", "Lem0nka Tools").apply {
            setTarget("_blank")
            style.set(CSS.FONT_SIZE, 40.px)
            style.set(CSS.OUTLINE, "none")
        }
        val magnar = Anchor("https://github.com/MagnarIUK", "magnariuk").apply {
            setTarget("_blank")
            style.set(CSS.FONT_SIZE, 15.px)
            style.set(CSS.OUTLINE, "none")
        }

        val made_By = Paragraph(NativeLabel("Made by ").apply {
            style.set(CSS.FONT_SIZE, 15.px)
            style.set(CSS.OUTLINE, "none")
        }, magnar).apply {  }
        val title = HorizontalLayout(lamonka, made_By)



        val menu = HorizontalLayout(
            Button("Головна").apply {
                addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
                onLeftClick {
                    UI.getCurrent().navigate(MainToolsView::class.java)
                }
            }

        ).apply {
            isSpacing = true
            alignItems = FlexComponent.Alignment.CENTER
        }

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
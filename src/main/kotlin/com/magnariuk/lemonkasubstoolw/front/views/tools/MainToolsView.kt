package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.github.mvysny.karibudsl.v10.onLeftClick
import com.magnariuk.lemonkasubstoolw.front.util.MainLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@PageTitle("Lemonka Tools")
@Route("/tools", layout = MainLayout::class)
class MainToolsView : VerticalLayout() {
    init {
        val button = Button("Розділення субтитрів").apply {
            onLeftClick {
                UI.getCurrent().navigate(SubsToolView::class.java)
            }
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        }
        val button2 = Button("Калькулятор слів").apply {
            onLeftClick {
                UI.getCurrent().navigate(ActorMeter::class.java)
            }
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        }

        add(button, button2)

        alignItems = Alignment.CENTER
        justifyContentMode = JustifyContentMode.CENTER
    }
}

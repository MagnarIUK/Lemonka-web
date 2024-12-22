package com.magnariuk.lemonkasubstoolw.data.util

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div

fun formatText(input: String): Component {
    val container = Div()

    val regex = Regex("\\{\\{(.*?):(.*?)}}")

    var lastIndex = 0
    regex.findAll(input).forEach { matchResult ->
        val matchStart = matchResult.range.first
        val matchEnd = matchResult.range.last + 1

        if(lastIndex < matchStart) {
            println("До: ${input.substring(lastIndex, matchStart)}")
            container.add(Text(input.substring(lastIndex, matchStart)))
        }

        val text = matchResult.groupValues[1]
        val link = matchResult.groupValues[2]

        container.add(Anchor(link, text).apply {
            setTarget("_blank")
        })
        println("Формат: $text:$link")

        lastIndex = matchEnd

    }

    if(lastIndex < input.length) {
        println("Після: ${input.substring(lastIndex)}")
        container.add(Text(input.substring(lastIndex)))
    }

    return container
}
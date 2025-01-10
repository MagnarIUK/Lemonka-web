package com.magnariuk.lemonkasubstoolw.data.util


object CSS {
    const val MIN_CONTENT = "min-content"
    const val MAX_CONTENT = "max-content"
    const val BACKGROUND_COLOR = "background-color"
    const val OVERFLOW_WRAP = "overflow-wrap"
    const val OVERFLOW = "overflow"
    const val BORDER_RADIUS = "border-radius"
    const val BORDER_COLOR = "border-color"
    const val BORDER = "border"
    const val TRANSITION = "transition"
    const val HEIGHT = "height"
    const val WIDTH = "width"
    const val MAX_WIDTH = "max-width"
    const val COLOR = "color"
    const val PADDING = "padding"
    const val MARGIN = "margin"
    const val MARGIN_BOTTOM = "margin-bottom"
    const val MARGIN_LEFT = "margin-left"
    const val MARGIN_RIGHT = "margin-right"
    const val SOLID = "solid"
    const val FONT_SIZE = "font-size"
    const val OUTLINE = "outline"
}
object COLORS{
    const val BLUE = "blue"
    const val RED = "red"
}
object OVERFLOW {
    const val AUTO ="auto"
}
object OVERFLOW_WRAP{
    const val BREAK_WORD = "break-word"
}

class ELEMENT(val properties: MutableList<String> = mutableListOf()) {
    fun add(property: String): ELEMENT {
        properties.add(property)
        return this
    }
    fun css():String =properties.joinToString(separator = " ")
}

object TRANSITION_D{
    const val ALL = "all"
    const val EASE = "ease"
}

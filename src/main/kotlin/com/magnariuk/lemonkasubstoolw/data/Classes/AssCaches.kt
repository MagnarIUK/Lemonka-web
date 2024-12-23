package com.magnariuk.lemonkasubstoolw.data.Classes

data class Ass(
    var subName: String = "",
    var scriptInfo: ScriptInfo,
    var aegisubProjectGarbage: AegisubProjectGarbage,
    var events: Events,
    var styles: Styles
) {
    fun getAllActors(): List<String> {
        return events.dialogues.map { it.actor }.distinct()
    }
}

data class Styles(
    var format: String, // Format line in styles section
    var styleList: List<Style> // List of individual styles
)

data class Style(
    var name: String,
    var fontName: String,
    var fontSize: Int,
    var primaryColour: String,
    var secondaryColour: String,
    var outlineColour: String,
    var backColour: String,
    var bold: Int,
    var italic: Int,
    var underline: Int,
    var strikeOut: Int,
    var scaleX: Int,
    var scaleY: Int,
    var spacing: Int,
    var angle: Int,
    var borderStyle: Int,
    var outline: Number,
    var shadow: Number,
    var alignment: Int,
    var marginL: Int,
    var marginR: Int,
    var marginV: Int,
    var encoding: Int
)

data class Comment(
    val layer: Int,
    val startTime: String,
    val endTime: String,
    val style: String,
    val actor: String,
    val marginL: Int,
    val marginR: Int,
    val marginV: Int,
    val effect: String,
    val text: String
)
data class Dialogue(
    val layer: Int,
    val startTime: String,
    val endTime: String,
    val style: String,
    val actor: String,
    val marginL: Int,
    val marginR: Int,
    val marginV: Int,
    val effect: String,
    val text: String
)
data class ScriptInfo(
    val Title: String = "",
    val ScriptType: String = "",
    val WrapStyle: Int = 0,
    val ScaledBorderAndShadow: String = "",
    val PlayResX: Int = 0,
    val PlayResY: Int = 0,
    val YCbCrMatrix: String = "",
)


data class AegisubProjectGarbage(
    val LastStyleStorage: String,
    val AudioFile: String,
    val VideoFile: String,
    val VideoARMode: String,
    val VideoARValue: Double,
    val VideoZoomPercent: Double,
    val ScrollPosition: Int,
    val ActiveLine: Int,
    val VideoPosition: Int
)

data class Events(
    var format: List<String>,
    var comments: List<Comment>,
    var dialogues: List<Dialogue>,
)



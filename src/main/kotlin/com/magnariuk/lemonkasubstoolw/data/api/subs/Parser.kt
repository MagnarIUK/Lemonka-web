package com.magnariuk.lemonkasubstoolw.data.api.subs

import com.magnariuk.lemonkasubstoolw.data.Classes.*
import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import com.magnariuk.lemonkasubstoolw.data.util.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets


class ParserIS {
    fun parseAssFile(inputStream: InputStream, filename: String): Ass {
        val scriptInfo = mutableMapOf<String, String>()
        val aegisubProjectGarbage = mutableMapOf<String, String>()
        val events = mutableListOf<Any>()
        val styles = mutableListOf<Style>()
        val eventFormatFields = mutableListOf<String>()
        var styleFormatFields = listOf<String>()

        var currentSection = ""

        inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                val trimmedLine = line.replace("\uFEFF", "").trim()
                when {
                    trimmedLine.startsWith("[") && trimmedLine.endsWith("]") -> {
                        currentSection = trimmedLine
                    }
                    currentSection == "[Script Info]" -> {
                        if (!trimmedLine.startsWith(";")) {
                            parseKeyValue(trimmedLine)?.let { (key, value) ->
                                scriptInfo[key] = value
                            }
                        }                    }
                    currentSection == "[Aegisub Project Garbage]" -> {
                        parseKeyValue(line)?.let { (key, value) -> aegisubProjectGarbage[key] = value }
                    }
                    currentSection == "[Events]" -> {
                        when {
                            line.startsWith("Format:") -> {
                                eventFormatFields.addAll(line.removePrefix("Format:").split(",").map { it.trim() })
                            }
                            line.startsWith("Comment:") -> {
                                events.add(parseEvent(line.removePrefix("Comment:"), eventFormatFields, isComment = true))
                            }
                            line.startsWith("Dialogue:") -> {
                                events.add(parseEvent(line.removePrefix("Dialogue:"), eventFormatFields, isComment = false))
                            }
                        }
                    }
                    currentSection == "[V4+ Styles]" -> {
                        when {
                            line.startsWith("Format:") -> {
                                styleFormatFields = line.removePrefix("Format:").split(",").map { it.trim() }
                            }
                            line.startsWith("Style:") -> {
                                styles.add(parseStyle(line.removePrefix("Style:"), styleFormatFields))
                            }
                        }
                    }
                }
            }
        }

        return Ass(
            subName = filename,
            scriptInfo = parseScriptInfo(scriptInfo),
            aegisubProjectGarbage = parseAegisubProjectGarbage(aegisubProjectGarbage),
            events = Events(
                format = eventFormatFields,
                comments = events.filterIsInstance<Comment>(),
                dialogues = events.filterIsInstance<Dialogue>()
            ),
            styles = Styles(
                format = styleFormatFields.joinToString(", "),
                styleList = styles
            )
        )
    }

    fun parseStyle(line: String, formatFields: List<String>): Style {
        val values = line.split(",").map { it.trim() }

        return Style(
            name = values[formatFields.indexOf("Name")],
            fontName = values[formatFields.indexOf("Fontname")],
            fontSize = values[formatFields.indexOf("Fontsize")].toInt(),
            primaryColour = values[formatFields.indexOf("PrimaryColour")],
            secondaryColour = values[formatFields.indexOf("SecondaryColour")],
            outlineColour = values[formatFields.indexOf("OutlineColour")],
            backColour = values[formatFields.indexOf("BackColour")],
            bold = values[formatFields.indexOf("Bold")].toInt(),
            italic = values[formatFields.indexOf("Italic")].toInt(),
            underline = values[formatFields.indexOf("Underline")].toInt(),
            strikeOut = values[formatFields.indexOf("StrikeOut")].toInt(),
            scaleX = values[formatFields.indexOf("ScaleX")].toInt(),
            scaleY = values[formatFields.indexOf("ScaleY")].toInt(),
            spacing = values[formatFields.indexOf("Spacing")].toInt(),
            angle = values[formatFields.indexOf("Angle")].toFloat(),
            borderStyle = values[formatFields.indexOf("BorderStyle")].toInt(),
            outline = values[formatFields.indexOf("Outline")].toFloat(),
            shadow = values[formatFields.indexOf("Shadow")].toFloat(),
            alignment = values[formatFields.indexOf("Alignment")].toInt(),
            marginL = values[formatFields.indexOf("MarginL")].toInt(),
            marginR = values[formatFields.indexOf("MarginR")].toInt(),
            marginV = values[formatFields.indexOf("MarginV")].toInt(),
            encoding = values[formatFields.indexOf("Encoding")].toInt()
        )
    }

    fun parseKeyValue(line: String): Pair<String, String>? {
        val parts = line.split(":", limit = 2).map { it.trim() }
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            parts[0] to parts[1]
        } else null
    }

    fun parseScriptInfo(data: Map<String, String>) = ScriptInfo(
        Title = data["Title"] ?: "",
        ScriptType = data["ScriptType"] ?: "",
        WrapStyle = data["WrapStyle"]?.toIntOrNull() ?: 0,
        ScaledBorderAndShadow = data["ScaledBorderAndShadow"] ?: "",
        PlayResX = data["PlayResX"]?.toIntOrNull() ?: 0,
        PlayResY = data["PlayResY"]?.toIntOrNull() ?: 0,
        YCbCrMatrix = data["YCbCr Matrix"] ?: ""
    )

    fun parseAegisubProjectGarbage(data: Map<String, String>) = AegisubProjectGarbage(
        LastStyleStorage = data["Last Style Storage"] ?: "",
        AudioFile = data["Audio File"] ?: "",
        VideoFile = data["Video File"] ?: "",
        VideoARMode = data["Video AR Mode"] ?: "",
        VideoARValue = data["Video AR Value"]?.toDoubleOrNull() ?: 0.0,
        VideoZoomPercent = data["Video Zoom Percent"]?.toDoubleOrNull() ?: 0.0,
        ScrollPosition = data["Scroll Position"]?.toIntOrNull() ?: 0,
        ActiveLine = data["Active Line"]?.toIntOrNull() ?: 0,
        VideoPosition = data["Video Position"]?.toIntOrNull() ?: 0
    )

    fun parseEvent(line: String, format: List<String>, isComment: Boolean): Any {
        val values = line.split(",", limit = 10).map { it.trim() }

        val eventData = format.zip(values).toMap()

        return if (isComment) {
            Comment(
                layer = eventData["Layer"]?.toIntOrNull() ?: 0,
                startTime = eventData["Start"] ?: "",
                endTime = eventData["End"] ?: "",
                style = eventData["Style"] ?: "",
                actor = eventData["Name"] ?: "",
                marginL = eventData["MarginL"]?.toIntOrNull() ?: 0,
                marginR = eventData["MarginR"]?.toIntOrNull() ?: 0,
                marginV = eventData["MarginV"]?.toIntOrNull() ?: 0,
                effect = eventData["Effect"] ?: "",
                text = eventData["Text"] ?: ""
            )
        } else {
            Dialogue(
                layer = eventData["Layer"]?.toIntOrNull() ?: 0,
                startTime = eventData["Start"] ?: "",
                endTime = eventData["End"] ?: "",
                style = eventData["Style"] ?: "",
                actor = eventData["Name"] ?: "",
                marginL = eventData["MarginL"]?.toIntOrNull() ?: 0,
                marginR = eventData["MarginR"]?.toIntOrNull() ?: 0,
                marginV = eventData["MarginV"]?.toIntOrNull() ?: 0,
                effect = eventData["Effect"] ?: "",
                text = eventData["Text"] ?: ""
            )
        }
    }

    fun createAss(filename: String, ass: Ass, characters: List<String>, separators: List<String>): StreamResource? {
        val lines: MutableList<String> = mutableListOf()
        if (ass.events.dialogues.any { it.actor in characters }) {
            lines.add("[Script Info]")
            lines.add("Title: ${ass.scriptInfo.Title}")
            lines.add("ScriptType: ${ass.scriptInfo.ScriptType}")
            lines.add("WrapStyle: ${ass.scriptInfo.WrapStyle}")
            lines.add("ScaledBorderAndShadow: ${ass.scriptInfo.ScaledBorderAndShadow}")
            lines.add("PlayResX: ${ass.scriptInfo.PlayResX}")
            lines.add("PlayResY: ${ass.scriptInfo.PlayResY}")
            lines.add("YCbCr Matrix: ${ass.scriptInfo.YCbCrMatrix}")
            lines.add("")
            lines.add("[Aegisub Project Garbage]")
            lines.add("Last Style Storage: ${ass.aegisubProjectGarbage.LastStyleStorage}")
            lines.add("Audio File: ${ass.aegisubProjectGarbage.AudioFile}")
            lines.add("Video File: ${ass.aegisubProjectGarbage.VideoFile}")
            lines.add("Video AR Mode: ${ass.aegisubProjectGarbage.VideoARMode}")
            lines.add("Video AR Value: ${ass.aegisubProjectGarbage.VideoARValue}")
            lines.add("Video Zoom Percent: ${ass.aegisubProjectGarbage.VideoZoomPercent}")
            lines.add("Scroll Position: ${ass.aegisubProjectGarbage.ScrollPosition}")
            lines.add("Active Line: ${ass.aegisubProjectGarbage.ActiveLine}")
            lines.add("Video Position: ${ass.aegisubProjectGarbage.VideoPosition}")
            lines.add("")
            lines.add("[V4+ Styles]")
            lines.add("Format: ${ass.styles.format}")
            val formatStyles = ass.styles.format.split(",").map { it.trim() }
            ass.styles.styleList.forEach { style ->
                val styleValues = formatStyles.map { field ->
                    when (field) {
                        "Name" -> style.name
                        "Fontname" -> style.fontName
                        "Fontsize" -> style.fontSize.toString()
                        "PrimaryColour" -> style.primaryColour
                        "SecondaryColour" -> style.secondaryColour
                        "OutlineColour" -> style.outlineColour
                        "BackColour" -> style.backColour
                        "Bold" -> style.bold
                        "Italic" -> style.italic
                        "Underline" -> style.underline
                        "StrikeOut" -> style.strikeOut
                        "ScaleX" -> style.scaleX.toString()
                        "ScaleY" -> style.scaleY.toString()
                        "Spacing" -> style.spacing.toString()
                        "Angle" -> style.angle.toString()
                        "BorderStyle" -> style.borderStyle.toString()
                        "Outline" -> style.outline.toString()
                        "Shadow" -> style.shadow.toString()
                        "Alignment" -> style.alignment.toString()
                        "MarginL" -> style.marginL.toString()
                        "MarginR" -> style.marginR.toString()
                        "MarginV" -> style.marginV.toString()
                        "Encoding" -> style.encoding.toString()
                        else -> ""//TODO
                    }
                }
                lines.add("Style: ${styleValues.joinToString(",")}")
            }
            lines.add("")
            lines.add("[Events]")
            val formatEvents = ass.events.format.map { it.trim() }
            lines.add("Format: ${formatEvents.joinToString(",")}")
            ass.events.comments.forEach { comment ->
                val commentValues = formatEvents.map { field ->
                    when(field) {
                        "Layer" -> comment.layer.toString()
                        "Start" -> comment.startTime
                        "End" -> comment.endTime
                        "Style" -> comment.style
                        "MarginL" -> comment.marginL
                        "MarginR" -> comment.marginR
                        "MarginV" -> comment.marginV
                        "Effect" -> comment.effect
                        "Text" -> comment.text
                        else -> ""//TODO
                    }
                }
                lines.add("Comment: ${commentValues.joinToString(",")}")
            }
            ass.events.dialogues.forEach { dialogue ->
                val chars = dialogue.actor.split(*separators.toTypedArray()).map { it.trim() }
                val filteredActors = chars.filter { it in characters }
                if(filteredActors.isNotEmpty() && !lines.contains("${dialogue.startTime},${dialogue.endTime}")){
                    val dialogueValues = formatEvents.map { field ->
                        when(field) {
                            "Layer" -> dialogue.layer.toString()
                            "Start" -> dialogue.startTime
                            "End" -> dialogue.endTime
                            "Style" -> dialogue.style
                            "Name" -> filteredActors.joinToString(" and ")
                            "MarginL" -> dialogue.marginL
                            "MarginR" -> dialogue.marginR
                            "MarginV" -> dialogue.marginV
                            "Effect" -> dialogue.effect
                            "Text" -> dialogue.text
                            else -> ""//TODO
                        }
                    }
                    lines.add("Dialogue: ${dialogueValues.joinToString(",")}")
                }
            }
            return generateStreamResource(filename, lines)
        }
        return null
    }

    fun renameActors(ass: Ass, actors: MutableList<Actor>, separators: List<String>): StreamResource?{
        val name = ass.subName
        val name_ = File(ass.subName).nameWithoutExtension
        val ex_ = File(ass.subName).extension

        var newAss = ass.copy(subName = name_+"_renamed."+ex_)

        ass.events.dialogues.forEach { dialogue ->

            val actors_splitted = dialogue.actor.split(*separators.toTypedArray()).map { it.trim() }

            val characterToActorMap = mutableMapOf<String, String>()
            actors.forEach { actor ->
                actor.characterNames.forEach { character ->
                    characterToActorMap[character] = actor.actorName
                }
            }

            val converted: MutableList<String> = mutableListOf()
            actors_splitted.forEach { character ->
                val newActorName = characterToActorMap[character]
                if (newActorName != null) {
                    converted.add(newActorName)
                } else {
                    converted.add(character)
                }
            }

            val newDialogue = converted.joinToString(separator = " та ")
            dialogue.actor = newDialogue
        }


        val lines: MutableList<String> = mutableListOf()
        lines.add("[Script Info]")
        lines.add("Title: ${newAss.scriptInfo.Title}")
        lines.add("ScriptType: ${newAss.scriptInfo.ScriptType}")
        lines.add("WrapStyle: ${newAss.scriptInfo.WrapStyle}")
        lines.add("ScaledBorderAndShadow: ${newAss.scriptInfo.ScaledBorderAndShadow}")
        lines.add("PlayResX: ${newAss.scriptInfo.PlayResX}")
        lines.add("PlayResY: ${newAss.scriptInfo.PlayResY}")
        lines.add("YCbCr Matrix: ${newAss.scriptInfo.YCbCrMatrix}")
        lines.add("")
        lines.add("[Aegisub Project Garbage]")
        lines.add("Last Style Storage: ${newAss.aegisubProjectGarbage.LastStyleStorage}")
        lines.add("Audio File: ${newAss.aegisubProjectGarbage.AudioFile}")
        lines.add("Video File: ${newAss.aegisubProjectGarbage.VideoFile}")
        lines.add("Video AR Mode: ${newAss.aegisubProjectGarbage.VideoARMode}")
        lines.add("Video AR Value: ${newAss.aegisubProjectGarbage.VideoARValue}")
        lines.add("Video Zoom Percent: ${newAss.aegisubProjectGarbage.VideoZoomPercent}")
        lines.add("Scroll Position: ${newAss.aegisubProjectGarbage.ScrollPosition}")
        lines.add("Active Line: ${newAss.aegisubProjectGarbage.ActiveLine}")
        lines.add("Video Position: ${newAss.aegisubProjectGarbage.VideoPosition}")
        lines.add("")
        lines.add("[V4+ Styles]")
        lines.add("Format: ${newAss.styles.format}")

        val formatStyles = newAss.styles.format.split(",").map { it.trim() }
        newAss.styles.styleList.forEach { style ->
            val styleValues = formatStyles.map { field ->
                when (field) {
                    "Name" -> style.name
                    "Fontname" -> style.fontName
                    "Fontsize" -> style.fontSize.toString()
                    "PrimaryColour" -> style.primaryColour
                    "SecondaryColour" -> style.secondaryColour
                    "OutlineColour" -> style.outlineColour
                    "BackColour" -> style.backColour
                    "Bold" -> style.bold
                    "Italic" -> style.italic
                    "Underline" -> style.underline
                    "StrikeOut" -> style.strikeOut
                    "ScaleX" -> style.scaleX.toString()
                    "ScaleY" -> style.scaleY.toString()
                    "Spacing" -> style.spacing.toString()
                    "Angle" -> style.angle.toString()
                    "BorderStyle" -> style.borderStyle.toString()
                    "Outline" -> style.outline.toString()
                    "Shadow" -> style.shadow.toString()
                    "Alignment" -> style.alignment.toString()
                    "MarginL" -> style.marginL.toString()
                    "MarginR" -> style.marginR.toString()
                    "MarginV" -> style.marginV.toString()
                    "Encoding" -> style.encoding.toString()
                    else -> ""//TODO
                }
            }
            lines.add("Style: ${styleValues.joinToString(",")}")
        }
        lines.add("")
        lines.add("[Events]")
        val formatEvents = newAss.events.format.map { it.trim() }
        lines.add("Format: ${formatEvents.joinToString(",")}")
        newAss.events.comments.forEach { comment ->
            val commentValues = formatEvents.map { field ->
                when(field) {
                    "Layer" -> comment.layer.toString()
                    "Start" -> comment.startTime
                    "End" -> comment.endTime
                    "Style" -> comment.style
                    "MarginL" -> comment.marginL
                    "MarginR" -> comment.marginR
                    "MarginV" -> comment.marginV
                    "Effect" -> comment.effect
                    "Text" -> comment.text
                    else -> ""//TODO
                }
            }
            lines.add("Comment: ${commentValues.joinToString(",")}")
        }

        newAss.events.dialogues.forEach { dialogue ->
                val dialogueValues = formatEvents.map { field ->
                    when(field) {
                        "Layer" -> dialogue.layer.toString()
                        "Start" -> dialogue.startTime
                        "End" -> dialogue.endTime
                        "Style" -> dialogue.style
                        "Name" -> dialogue.actor
                        "MarginL" -> dialogue.marginL
                        "MarginR" -> dialogue.marginR
                        "MarginV" -> dialogue.marginV
                        "Effect" -> dialogue.effect
                        "Text" -> dialogue.text
                        else -> ""//TODO
                    }
                }
                lines.add("Dialogue: ${dialogueValues.joinToString(",")}")
            }

        return generateStreamResource(newAss.subName, lines)
    }

    fun createSubRip(filename: String, ass: Ass, characters: List<String>, separators: List<String>): StreamResource?{
        val lines: MutableList<String> = mutableListOf()
        var counter = 2
        if (ass.events.dialogues.any { it.actor in characters }) {
            lines.add("1")
            lines.add("00:00:00,00 --> 00:00:00,1")
            lines.add("   ")
            lines.add("")

            ass.events.dialogues.forEach { dialogue ->
                val chars = dialogue.actor.split(*separators.toTypedArray()).map { it.trim() }
                val filteredActors = chars.filter { it in characters }

                if(filteredActors.isNotEmpty() && !lines.contains("${dialogue.startTime} --> ${dialogue.endTime}")){
                    lines.add(counter.toString())
                    lines.add("0${dialogue.startTime.replace(".", ",")} --> 0${dialogue.endTime.replace(".", ",")}")
                    lines.add("[${filteredActors.joinToString(", ")}]: ${dialogue.text.replace(Regex("\\{.*?}"), "")}") //.replace(Regex("\\{.*?}"), "")
                    lines.add("")
                    counter++
                }
            }
            return generateStreamResource(filename, lines)
        }
        return null
    }

}

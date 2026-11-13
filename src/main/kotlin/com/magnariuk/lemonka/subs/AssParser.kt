package com.magnariuk.lemonka.subs

import java.io.InputStream

/** Reads Advanced SubStation (.ass) files. Ported from the old ParserIS (parsing part, behaviour unchanged). */
object AssParser {
    fun parse(input: InputStream, filename: String): Ass {
        val scriptInfo = mutableMapOf<String, String>()
        val garbage = mutableMapOf<String, String>()
        val events = mutableListOf<Any>()
        val styles = mutableListOf<Style>()
        val eventFormat = mutableListOf<String>()
        var styleFormat = listOf<String>()
        var section = ""

        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { raw ->
                val line = raw.replace("\uFEFF", "")
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("[") && trimmed.endsWith("]") -> section = trimmed
                    section == "[Script Info]" ->
                        if (!trimmed.startsWith(";")) parseKeyValue(trimmed)?.let { (k, v) -> scriptInfo[k] = v }
                    section == "[Aegisub Project Garbage]" -> parseKeyValue(line)?.let { (k, v) -> garbage[k] = v }
                    section == "[Events]" -> when {
                        line.startsWith("Format:") -> eventFormat.addAll(line.removePrefix("Format:").split(",").map { it.trim() })
                        line.startsWith("Comment:") -> events.add(parseEvent(line.removePrefix("Comment:"), eventFormat, true))
                        line.startsWith("Dialogue:") -> events.add(parseEvent(line.removePrefix("Dialogue:"), eventFormat, false))
                    }
                    section == "[V4+ Styles]" -> when {
                        line.startsWith("Format:") -> styleFormat = line.removePrefix("Format:").split(",").map { it.trim() }
                        line.startsWith("Style:") -> styles.add(parseStyle(line.removePrefix("Style:"), styleFormat))
                    }
                }
            }
        }

        return Ass(
            subName = filename,
            scriptInfo = parseScriptInfo(scriptInfo),
            aegisubProjectGarbage = parseGarbage(garbage),
            events = Events(eventFormat, events.filterIsInstance<Comment>(), events.filterIsInstance<Dialogue>()),
            styles = Styles(styleFormat.joinToString(", "), styles),
        )
    }

    private fun parseStyle(line: String, f: List<String>): Style {
        val v = line.split(",").map { it.trim() }
        fun s(name: String) = v[f.indexOf(name)]
        return Style(
            name = s("Name"), fontName = s("Fontname"), fontSize = s("Fontsize").toInt(),
            primaryColour = s("PrimaryColour"), secondaryColour = s("SecondaryColour"),
            outlineColour = s("OutlineColour"), backColour = s("BackColour"),
            bold = s("Bold").toInt(), italic = s("Italic").toInt(), underline = s("Underline").toInt(),
            strikeOut = s("StrikeOut").toInt(), scaleX = s("ScaleX").toInt(), scaleY = s("ScaleY").toInt(),
            spacing = s("Spacing").toInt(), angle = s("Angle").toFloat(), borderStyle = s("BorderStyle").toInt(),
            outline = s("Outline").toFloat(), shadow = s("Shadow").toFloat(), alignment = s("Alignment").toInt(),
            marginL = s("MarginL").toInt(), marginR = s("MarginR").toInt(), marginV = s("MarginV").toInt(),
            encoding = s("Encoding").toInt(),
        )
    }

    private fun parseKeyValue(line: String): Pair<String, String>? {
        val parts = line.split(":", limit = 2).map { it.trim() }
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) parts[0] to parts[1] else null
    }

    private fun parseScriptInfo(d: Map<String, String>) = ScriptInfo(
        Title = d["Title"] ?: "", ScriptType = d["ScriptType"] ?: "", WrapStyle = d["WrapStyle"]?.toIntOrNull() ?: 0,
        ScaledBorderAndShadow = d["ScaledBorderAndShadow"] ?: "", PlayResX = d["PlayResX"]?.toIntOrNull() ?: 0,
        PlayResY = d["PlayResY"]?.toIntOrNull() ?: 0, YCbCrMatrix = d["YCbCr Matrix"] ?: "",
    )

    private fun parseGarbage(d: Map<String, String>) = AegisubProjectGarbage(
        LastStyleStorage = d["Last Style Storage"] ?: "", AudioFile = d["Audio File"] ?: "",
        VideoFile = d["Video File"] ?: "", VideoARMode = d["Video AR Mode"] ?: "",
        VideoARValue = d["Video AR Value"]?.toDoubleOrNull() ?: 0.0,
        VideoZoomPercent = d["Video Zoom Percent"]?.toDoubleOrNull() ?: 0.0,
        ScrollPosition = d["Scroll Position"]?.toIntOrNull() ?: 0, ActiveLine = d["Active Line"]?.toIntOrNull() ?: 0,
        VideoPosition = d["Video Position"]?.toIntOrNull() ?: 0,
    )

    private fun parseEvent(line: String, format: List<String>, isComment: Boolean): Any {
        val e = format.zip(line.split(",", limit = 10).map { it.trim() }).toMap()
        val layer = e["Layer"]?.toIntOrNull() ?: 0
        val start = e["Start"] ?: ""; val end = e["End"] ?: ""; val style = e["Style"] ?: ""; val actor = e["Name"] ?: ""
        val ml = e["MarginL"]?.toIntOrNull() ?: 0; val mr = e["MarginR"]?.toIntOrNull() ?: 0; val mv = e["MarginV"]?.toIntOrNull() ?: 0
        val effect = e["Effect"] ?: ""; val text = e["Text"] ?: ""
        return if (isComment) Comment(layer, start, end, style, actor, ml, mr, mv, effect, text)
        else Dialogue(layer, start, end, style, actor, ml, mr, mv, effect, text)
    }
}

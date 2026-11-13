package com.magnariuk.lemonka.subs

import java.io.InputStream

/**
 * Reads SubRip (.srt) files. Cues are blocks separated by blank lines:
 * ```
 * 2
 * 00:00:02,098 --> 00:00:04,015
 * [Shuri]: Маму-ся!
 * ```
 * A leading `[Name, Other]:` marks who speaks; the raw name string is kept and split later with the
 * configured separators (see [ActorNames]).
 */
object SrtParser {
    private val timing = Regex("""(.*?)\s*-->\s*(.*)""")
    private val speaker = Regex("""^\[(.*?)]:\s*(.*)$""")

    fun parse(input: InputStream, filename: String): SRT {
        val text = input.bufferedReader(Charsets.UTF_8).readText().replace("\uFEFF", "").replace("\r\n", "\n")
        val cues = text.split(Regex("\n\\s*\n")).mapNotNull { block -> parseCue(block.lines().filter { it.isNotBlank() }) }
        return SRT(filename, cues.toMutableList())
    }

    private fun parseCue(lines: List<String>): SrtDialogue? {
        val timeIdx = lines.indexOfFirst { "-->" in it }
        if (timeIdx < 0) return null
        val number = lines.getOrNull(timeIdx - 1)?.trim()?.toIntOrNull()
        val (start, end) = timing.find(lines[timeIdx])?.destructured ?: return null
        val body = lines.drop(timeIdx + 1)
        if (body.isEmpty()) return SrtDialogue(number, start, end, null)

        // Old behaviour kept only the first text line; multi-line cues are now joined.
        val first = speaker.find(body.first().trim())
        val names = first?.groupValues?.get(1)
        val textLines = listOfNotNull(first?.groupValues?.get(2) ?: body.first()) + body.drop(1)
        return SrtDialogue(number, start, end, textLines.joinToString(" ") { it.trim() }, rawActors = names)
    }
}

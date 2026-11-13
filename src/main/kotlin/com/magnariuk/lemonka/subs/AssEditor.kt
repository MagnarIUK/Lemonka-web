package com.magnariuk.lemonka.subs

/**
 * Edits an .ass file by rewriting only its `Dialogue:` lines and leaving every other byte as it was
 * (styles, fonts, comments, extra sections, line endings). The old version rebuilt the file from parsed
 * data, which dropped unknown sections and the Name field of comments.
 */
class AssEditor(text: String) {
    private val newline = if ("\r\n" in text) "\r\n" else "\n"
    private val lines = text.removePrefix("\uFEFF").split(Regex("\r?\n"))

    /** Keeps only lines spoken by [characters]; the Name field lists just those characters (joined with " and "). */
    fun filterTo(characters: Set<String>, separators: List<String>): String? {
        var any = false
        val out = mapDialogues { name ->
            val kept = ActorNames.split(name, separators).filter { it in characters }
            if (kept.isEmpty()) null else { any = true; kept.joinToString(" and ") }
        }
        return if (any) out else null
    }

    /** Replaces character names with their actors' names (joined with " та "); unknown names stay. */
    fun renameToActors(characterToActor: Map<String, String>, separators: List<String>): String =
        mapDialogues { name ->
            ActorNames.split(name, separators).map { characterToActor[it] ?: it }.joinToString(" та ")
        }

    /** @param rename returns the new Name, or null to drop the line. */
    private fun mapDialogues(rename: (String) -> String?): String {
        var section = ""
        var format = emptyList<String>()
        val result = ArrayList<String>(lines.size)
        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("[") && t.endsWith("]")) section = t
            if (section == "[Events]" && line.startsWith("Format:")) {
                format = line.removePrefix("Format:").split(",").map { it.trim() }
            }
            if (section == "[Events]" && line.startsWith("Dialogue:") && "Name" in format) {
                val prefix = line.substring(0, "Dialogue:".length)
                val body = line.substring(prefix.length)
                val values = body.split(",", limit = format.size).toMutableList()
                val i = format.indexOf("Name")
                if (i < values.size) {
                    val newName = rename(values[i].trim()) ?: continue
                    values[i] = newName
                    result += prefix + values.joinToString(",")
                    continue
                }
            }
            result += line
        }
        return result.joinToString(newline)
    }
}

/** Turns .ass dialogue into .srt, with correct millisecond timings. */
object SrtWriter {
    private val tags = Regex("""\{.*?}""")

    fun fromAss(ass: Ass, characters: Set<String>, separators: List<String>): String? {
        val cues = ass.events.dialogues.mapNotNull { d ->
            val kept = ActorNames.split(d.actor, separators).filter { it in characters }
            if (kept.isEmpty()) null else Triple(d, kept, d.text.replace(tags, "").replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " "))
        }
        if (cues.isEmpty()) return null
        return buildString {
            // Empty first cue, as the old version produced (some tools expect it).
            append("1\n00:00:00,000 --> 00:00:00,001\n \n\n")
            cues.forEachIndexed { i, (d, names, text) ->
                append(i + 2).append('\n')
                append(time(d.startTime)).append(" --> ").append(time(d.endTime)).append('\n')
                append('[').append(names.joinToString(", ")).append("]: ").append(text).append("\n\n")
            }
        }.trimEnd() + "\n"
    }

    /** ASS "0:00:01.50" (centiseconds) → SRT "00:00:01,500". The old code wrote "00:00:01,50" = 50 ms. */
    fun time(ass: String): String {
        val m = Regex("""(\d+):(\d{1,2}):(\d{1,2})[.,](\d{1,3})""").find(ass.trim()) ?: return "00:00:00,000"
        val (h, min, s, frac) = m.destructured
        val ms = frac.padEnd(3, '0').take(3)
        return "%02d:%02d:%02d,%s".format(h.toInt(), min.toInt(), s.toInt(), ms)
    }
}

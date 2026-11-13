package com.magnariuk.lemonka.subs

/** Splits a speaker field like "Fern та Stark" into names using the admin-configured separators. */
object ActorNames {
    fun split(raw: String?, separators: List<String>): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        if (separators.isEmpty()) return listOf(raw.trim())
        val pattern = separators.filter { it.isNotBlank() }.joinToString("|") { sep ->
            val s = sep.trim()
            // Word separators ("and", "та") only match as whole words, so "Alexandra" stays one name.
            if (s.all { it.isLetter() }) "(?<!\\p{L})${Regex.escape(s)}(?!\\p{L})" else Regex.escape(s)
        }
        if (pattern.isEmpty()) return listOf(raw.trim())
        return raw.split(Regex(pattern)).map { it.trim() }.filter { it.isNotEmpty() }
    }
}

/** One spoken line, independent of file format. */
data class SpokenLine(val actors: List<String>, val text: String)

data class NumberWords(val number: Int, val words: Int)

data class FileCount(val file: String, val lines: Int, val words: Int, val numbers: List<NumberWords>) {
    val numberWords get() = numbers.sumOf { it.words }
    val total get() = words + numberWords
}

/**
 * Counts lines ("репліки"), words and numbers. Same rules as the old ActorMeter:
 * - a line spoken by N characters counts as N lines, and its words count N times;
 * - numbers are listed separately with how many words they are when read aloud in Ukrainian.
 */
object WordCounter {
    private val overrideTags = Regex("""\{[^}]*}""")

    fun linesOf(ass: Ass, separators: List<String>) =
        ass.events.dialogues.map { SpokenLine(ActorNames.split(it.actor, separators), it.text) }

    fun linesOf(srt: SRT, separators: List<String>) =
        srt.dialogs.map { SpokenLine(ActorNames.split(it.rawActors, separators), it.dialog.orEmpty()) }

    /** @param characters only count lines where one of these speaks; empty = everyone. */
    fun count(file: String, lines: List<SpokenLine>, characters: Set<String>): FileCount {
        val selected = if (characters.isEmpty()) lines else lines.filter { l -> l.actors.any { it in characters } }
        var lineCount = 0
        var words = 0
        val numbers = mutableListOf<NumberWords>()
        selected.forEach { line ->
            val speakers = line.actors.size.coerceAtLeast(1)
            lineCount += speakers
            tokens(line.text).forEach { token ->
                val n = token.toIntOrNull()
                if (n != null) numbers += NumberWords(n, numberToWordCount(n)) else words += speakers
            }
        }
        return FileCount(file, lineCount, words, numbers)
    }

    /** Words of a line: ASS tags like {\an8} removed, `\N` line breaks and commas treated as before. */
    fun tokens(text: String): List<String> =
        text.replace(overrideTags, " ").replace("\\h", " ").replace(",", "")
            .split(" ", "\\N", "\\n").filter { it.isNotBlank() }

    private fun numberToWordCount(n: Int) = try { countWordsInNumber(n) } catch (_: IndexOutOfBoundsException) { 0 }
}

/** The full result, plus the same plain-text report the old version offered as a download. */
data class CountReport(val characters: List<String>, val files: List<FileCount>) {
    val lines get() = files.sumOf { it.lines }
    val words get() = files.sumOf { it.words }
    val numberWords get() = files.sumOf { it.numberWords }
    val total get() = words + numberWords

    fun toText(): String = buildString {
        appendLine("Репліки: $lines")
        appendLine("Слова: $words")
        appendLine()
        appendLine(if (characters.isEmpty()) "Усі персонажі" else characters.joinToString(" | "))
        appendLine("Числа рекомендується перевірити вручну!")
        files.forEach { f ->
            appendLine(f.file)
            appendLine("Слова: ${f.words}")
            if (f.numbers.isEmpty()) { appendLine(); return@forEach }
            appendLine("Разом з числами: ${f.total}")
            appendLine("Числа:")
            f.numbers.forEach { appendLine("   ${it.number} = ${it.words}") }
            appendLine("Загалом: ${f.numberWords}")
            appendLine()
        }
        appendLine("Загалом числа: $numberWords")
        append("Загалом числа разом зі словами: $total")
    }
}

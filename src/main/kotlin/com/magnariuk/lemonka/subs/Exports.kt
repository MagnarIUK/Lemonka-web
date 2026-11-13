package com.magnariuk.lemonka.subs

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class SubFormat(val ext: String, val title: String) {
    Ass("ass", "Advanced SubStation Alpha (.ass)"),
    Srt("srt", "SubRip (.srt)");

    companion object { fun of(v: String?) = entries.firstOrNull { it.ext == v } ?: Ass }
}

enum class ExportKind(val title: String) {
    /** One file per actor with all of their characters (old "Створити субтитри"). */
    ByActor("Субтитри по акторах"),
    /** One file per character (old "Розділити по персонажам", which was unfinished). */
    ByCharacter("Субтитри по персонажах"),
    /** The whole file with character names replaced by actor names (old "Перейменувати акторів"). */
    Renamed("Перейменований файл");

    companion object { fun of(v: String?) = entries.firstOrNull { it.name.equals(v, true) } ?: ByActor }
}

/** @param lines how many spoken lines the file contains. */
data class ExportFile(val name: String, val content: String, val lines: Int = 0)

/** Builds output files from the original .ass text and the project's actor → characters mapping. */
class SubtitleExporter(
    private val fileName: String,
    private val assText: String,
    private val separators: List<String>,
) {
    private val ass by lazy { AssParser.parse(assText.byteInputStream(), fileName) }
    private val base = File(fileName).nameWithoutExtension

    fun export(kind: ExportKind, format: SubFormat, actors: Map<String, List<String>>): List<ExportFile> = when (kind) {
        ExportKind.ByActor -> actors.mapNotNull { (actor, chars) -> one(actor, chars.toSet(), format) }
        ExportKind.ByCharacter -> actors.values.flatten().distinct().sorted().mapNotNull { one(it, setOf(it), format) }
        ExportKind.Renamed -> {
            val map = actors.flatMap { (actor, chars) -> chars.map { it to actor } }.toMap()
            listOf(ExportFile("${base}_renamed.ass", AssEditor(assText).renameToActors(map, separators), ass.events.dialogues.size))
        }
    }

    private fun one(title: String, characters: Set<String>, format: SubFormat): ExportFile? {
        val content = when (format) {
            SubFormat.Ass -> AssEditor(assText).filterTo(characters, separators)
            SubFormat.Srt -> SrtWriter.fromAss(ass, characters, separators)
        } ?: return null
        val lines = ass.events.dialogues.count { d -> ActorNames.split(d.actor, separators).any { it in characters } }
        return ExportFile("${safe(title)}.${format.ext}", content, lines)
    }

    private fun safe(name: String) = name.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "unnamed" }

    companion object {
        fun zip(files: List<ExportFile>): ByteArray = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                val used = mutableSetOf<String>()
                files.forEach { f ->
                    var name = f.name
                    var n = 2
                    while (!used.add(name)) {
                        name = "${File(f.name).nameWithoutExtension} ($n).${File(f.name).extension}"
                        n++
                    }
                    zip.putNextEntry(ZipEntry(name)); zip.write(f.content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
                }
            }
        }.toByteArray()
    }
}

package com.magnariuk.lemonka.subs

import kotlin.test.*

class WordCounterTest {
    private val seps = listOf(";", ",", "and", "та")

    private val ass = """
        [Script Info]
        Title: Test

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        Dialogue: 0,0:00:01.00,0:00:02.00,Default,Fern,0,0,0,,Добрий день, пане Штарк
        Dialogue: 0,0:00:02.00,0:00:03.00,Default,Stark,0,0,0,,У мене 3 яблука\Nі 25 груш
        Dialogue: 0,0:00:03.00,0:00:04.00,Default,Frieren,0,0,0,,Ходімо далі
        Dialogue: 0,0:00:04.00,0:00:05.00,Default,Fern,0,0,0,,Так 2024
        Comment: 0,0:00:05.00,0:00:06.00,Default,Fern,0,0,0,,це коментар не рахується
    """.trimIndent()

    private val srt = """
        1
        00:00:01,000 --> 00:00:02,000
        [Shuri]: Маму-ся!

        2
        00:00:02,000 --> 00:00:03,000
        [Fina]: Облиш маму, Шурі! Їй 40 років

        3
        00:00:03,000 --> 00:00:04,000
        Без мовця тут три слова
    """.trimIndent()

    private fun newAss(chars: Set<String> = emptySet()) =
        WordCounter.count("a.ass", WordCounter.linesOf(AssParser.parse(ass.byteInputStream(), "a.ass"), seps), chars)

    private fun newSrt(text: String = srt, chars: Set<String> = emptySet()) =
        WordCounter.count("s.srt", WordCounter.linesOf(SrtParser.parse(text.byteInputStream(), "s.srt"), seps), chars)

    // ---------- same results as the old code on normal files ----------

    @Test
    fun `ass - all characters matches old`() {
        val old = OldASSCounter(AssParser.parse(ass.byteInputStream(), "a.ass"))
        val oldCount = old.count()
        val n = newAss()
        assertEquals(oldCount["words"]!!.toInt(), n.words)
        assertEquals(old.count_dialogs(), n.lines)
        assertEquals(oldCount["numbers"], n.numbers.joinToString(" ") { "${it.number}" })
        assertEquals(oldCount["numbers_counted"], n.numbers.joinToString(" ") { "${it.words}" })
    }

    @Test
    fun `ass - selected characters matches old`() {
        val old = OldASSCounter(AssParser.parse(ass.byteInputStream(), "a.ass"), mutableListOf("Fern"))
        val n = newAss(setOf("Fern"))
        assertEquals(old.count()["words"]!!.toInt(), n.words)
        assertEquals(old.count_dialogs(), n.lines)
        assertEquals(5, n.words) // "Добрий день пане Штарк" + "Так"; 2024 is a number
    }

    @Test
    fun `srt - all characters matches old`() {
        val old = OldSRTCounter(OldSRTParser(srt.byteInputStream(), "s.srt").parse())
        val oldCount = old.count(seps)
        val n = newSrt()
        assertEquals(oldCount["words"]!!.toInt(), n.words)
        assertEquals(old.count_dialogs(), n.lines)
        assertEquals(oldCount["numbers"], n.numbers.joinToString(" ") { "${it.number}" })
    }

    @Test
    fun `numbers are converted to spoken word counts`() {
        val n = newAss()
        assertEquals(listOf(3 to 1, 25 to 2, 2024 to 4), n.numbers.map { it.number to it.words })
        assertEquals(n.words + 7, n.total)
    }

    // ---------- bugs fixed on purpose ----------

    @Test
    fun `fixed - srt with selected characters is no longer multiplied`() {
        val old = OldSRTCounter(OldSRTParser(srt.byteInputStream(), "s.srt").parse(), mutableListOf("Fina")).count(seps)
        val n = newSrt(chars = setOf("Fina"))
        assertEquals(5, n.words) // "Облиш маму Шурі! Їй років" - 40 is a number
        assertNotEquals(n.words, old["words"]!!.toInt(), "old code counted every line's words once per selected line")
    }

    @Test
    fun `fixed - and inside a name does not split it`() {
        assertEquals(listOf("Alexandra"), ActorNames.split("Alexandra", seps))
        assertEquals(listOf("Fern", "Stark"), ActorNames.split("Fern та Stark", seps))
        assertEquals(listOf("A", "B", "C"), ActorNames.split("A, B and C", seps))
        assertEquals(listOf("Брандон"), ActorNames.split("Брандон", seps))
    }

    @Test
    fun `fixed - multi-character ass line counts when one of them is selected`() {
        val text = ass + "\nDialogue: 0,0:00:06.00,0:00:07.00,Default,Fern та Stark,0,0,0,,Разом сильніші"
        val lines = WordCounter.linesOf(AssParser.parse(text.byteInputStream(), "a.ass"), seps)
        val n = WordCounter.count("a.ass", lines, setOf("Fern"))
        assertEquals(5 + 2 * 2, n.words) // shared line: 2 words × 2 speakers, as before for multi-speaker lines
        assertEquals(2 + 2, n.lines)
    }

    @Test
    fun `fixed - multi-line srt cues and number-only lines`() {
        val text = """
            1
            00:00:01,000 --> 00:00:02,000
            [Shuri]: Перший рядок
            другий рядок

            2
            00:00:02,000 --> 00:00:03,000
            [Fina]: Скільки?
            42
        """.trimIndent()
        val n = newSrt(text)
        assertEquals(5, n.words)
        assertEquals(listOf(42), n.numbers.map { it.number })
        assertEquals(2, n.lines)
    }

    @Test
    fun `ass override tags are not words`() {
        assertEquals(listOf("Привіт", "світ"), WordCounter.tokens("""{\an8}Привіт {\i1}світ{\i0}"""))
    }

    @Test
    fun `report text keeps the old format`() {
        val r = CountReport(listOf("Fern"), listOf(newAss(setOf("Fern"))))
        val t = r.toText()
        assertTrue(t.startsWith("Репліки: 2\nСлова: 5\n\nFern\nЧисла рекомендується перевірити вручну!\na.ass\n"))
        assertContains(t, "   2024 = 4")
        assertTrue(t.endsWith("Загалом числа разом зі словами: 9"))
    }
}

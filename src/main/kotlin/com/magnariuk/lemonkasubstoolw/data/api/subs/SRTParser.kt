package com.magnariuk.lemonkasubstoolw.data.api.subs

import com.magnariuk.lemonkasubstoolw.data.Classes.SrtDialogue
import com.magnariuk.lemonkasubstoolw.data.Classes.SRT
import com.magnariuk.lemonkasubstoolw.data.util.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader




class SRTCounter( val srt: SRT){

    fun count_dialogs(): Int {
        var result = 0

        srt.dialogs.forEach {
            result+= it.actors.size
        }

        return result
    }

    fun count(): MutableMap<String, String> {
        val result: MutableMap<String, String>  = mutableMapOf()
        var count = 0
        val numbers: MutableList<Int>  = mutableListOf()

        srt.dialogs.forEach { dialog ->
            val actors = dialog.actors
            val texts = dialog.dialog?.replace(",", "")?.split(" ", "\\N")
            val actorsHere = actors.map { it.split("and", ",").map { it.trim() } }
            texts?.forEach { text ->
                var counterHere = 0
                if( text.toIntOrNull() != null ){
                    numbers.add(text.toInt())
                } else{
                    counterHere++
                }
                counterHere *= actorsHere.size

                count+=counterHere
            }
        }

        val numbers_counted: MutableList<Int> = mutableListOf()
        numbers.forEach {
            try {
                numbers_counted.add(countWordsInNumber(it))
            } catch (ex: ArrayIndexOutOfBoundsException){
                numbers_counted.add(0)
            }
        }


        result.put("words", count.toString())
        result.put("numbers", numbers.joinToString(" "))
        result.put("numbers_counted", numbers_counted.joinToString(" "))


        return result
    }
}

class SRTParser(val input: InputStream, val name: String) {
    fun parse(): SRT {
        var result: SRT = SRT()
        result.subName = name
        val reader = BufferedReader(InputStreamReader(input))
        var currentDialog = SrtDialogue()
        var sectionBefore = ""


        while (true) {
            val line = reader.readLine() ?: break

            when {
                line.toIntOrNull() != null -> {
                    if (currentDialog.number != null) {
                        result.dialogs.add(currentDialog)
                        currentDialog = SrtDialogue()
                    }
                    currentDialog.number = line.toInt()
                    sectionBefore = "number"
                }

                sectionBefore == "number" -> {
                    val regex = "(.*?) --> (.*)".toRegex()
                    val match = regex.find(line)
                    if (match != null) {
                        val start = match.groupValues[1]
                        val end = match.groupValues[2]
                        currentDialog.startTime = start
                        currentDialog.endTime = end
                    }
                    sectionBefore = "timestamp"
                }

                sectionBefore == "timestamp" -> {
                    val regex = "\\[(.*?)\\]: (.*)".toRegex()
                    val match = regex.find(line)
                    if (match != null) {
                        currentDialog.actors = match.groupValues[1]
                            .split(",", "and")
                            .map { it.trim() }
                            .toMutableList()
                        currentDialog.dialog = match.groupValues[2]
                    }
                }
            }
        }
        result.dialogs.add(currentDialog)
        return result
    }
}

/*\
1
00:00:00,000 --> 00:00:00,001


2
00:00:02,098 --> 00:00:04,015
[Shuri]: Маму-ся!

3
00:00:04,015 --> 00:00:05,082
[Fina]: Облиш маму, Шурі!

4
00:00:05,082 --> 00:00:08,036
[Fina]: Мамі треба ще відпочити!
*/
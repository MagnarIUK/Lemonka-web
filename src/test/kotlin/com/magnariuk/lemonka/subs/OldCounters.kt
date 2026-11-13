package com.magnariuk.lemonka.subs

// Verbatim copies of the OLD Vaadin-era counters, only used to compare results in tests.

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class OldSRT(var subName: String? = null, var dialogs: MutableList<OldSrtDialogue> = mutableListOf())
data class OldSrtDialogue(var number: Int? = null, var startTime: String? = null, var endTime: String? = null, var dialog: String? = null, var actors: MutableList<String> = mutableListOf())

class OldASSCounter(val ass: Ass, val characters: MutableList<String> = mutableListOf()) {
    fun count_dialogs(): Int {
        var result = 0

        if(characters.size == 0) {
            ass.events.dialogues.forEach {
                result += it.actor.split("and", ",").size
            }
        } else{
            ass.events.dialogues.filter { it.actor in characters }.forEach {
                result += it.actor.split("and", ",").size
            }
        }


        return result
    }

    fun count(): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        var count = 0
        val numbers: MutableList<Int> = mutableListOf()
        if (characters.size == 0) {
            ass.events.dialogues.forEach { dialog ->
                val actors = dialog.actor.split("and", ",")
                val texts = dialog.text.replace(",", "").split(" ", "\\N")
                val actorsHere = actors.map { it.split("and", "it").map { it.trim() } }

                texts.forEach { text ->
                    var counterHere = 0
                    if (text.toIntOrNull() != null) {
                        numbers.add(text.toInt())
                    } else{
                        counterHere++
                    }
                    counterHere *= actorsHere.size

                    count+=counterHere
                }
            }
        } else{
            ass.events.dialogues.filter { it.actor in characters }.forEach { dialog ->
                val actors = dialog.actor.split("and", ",")
                val texts = dialog.text.replace(",", "").split(" ", "\\N")
                val actorsHere = actors.map { it.split("and", "it").map { it.trim() } }

                texts.forEach { text ->
                    var counterHere = 0
                    if (text.toIntOrNull() != null) {
                        numbers.add(text.toInt())
                    } else{
                        counterHere++
                    }
                    counterHere *= actorsHere.size

                    count+=counterHere
                }
            }
        }


        val numbers_counted: MutableList<Int> = mutableListOf()
        numbers.forEach {
            try {
                numbers_counted.add(countWordsInNumber(it))
            } catch (ex: ArrayIndexOutOfBoundsException) {
                numbers_counted.add(0)
            }
        }

        result.put("words", count.toString())
        result.put("numbers", numbers.joinToString(" "))
        result.put("numbers_counted", numbers_counted.joinToString(" "))

        return result
    }


}class OldSRTCounter( val srt: OldSRT, val characters: MutableList<String> = mutableListOf()){

    fun count_dialogs(): Int {
        var result = 0
        if(characters.size == 0) {
            srt.dialogs.forEach {
                if(it.actors.size > 0){
                    result+= it.actors.size
                } else{
                    result+=1
                }
            }
        } else{
            srt.dialogs.filter { d ->
                d.actors.any { actor -> actor in characters }
            }.forEach {
                if(it.actors.size > 0){
                    result+= it.actors.size
                } else{
                    result+=1
                }
            }
        }

        return result
    }

    fun count(separators: List<String>): MutableMap<String, String> {
        val result: MutableMap<String, String>  = mutableMapOf()
        var count = 0
        val numbers: MutableList<Int>  = mutableListOf()

        srt.dialogs.forEach { dialog ->
            val actors = dialog.actors
            val texts = dialog.dialog?.replace(",", "")?.split(" ", "\\N")
            val actorsHere = actors.map { it.split(*separators.toTypedArray()).map { it.trim() } }



            if (characters.size == 0) {
                texts?.forEach { text ->
                    var counterHere = 0
                    if( text.toIntOrNull() != null ){
                        numbers.add(text.toInt())
                    } else{
                        counterHere++
                    }
                    if(actors.size > 0){
                        counterHere *= actorsHere.size
                    }

                    count+=counterHere
                }
            } else{
                srt.dialogs.filter { d ->
                    d.actors.any { actor -> actor in characters }
                }.forEach {
                    texts?.forEach { text ->
                        var counterHere = 0
                        if( text.toIntOrNull() != null ){
                            numbers.add(text.toInt())
                        } else{
                            counterHere++
                        }
                        if(actors.size > 0){
                            counterHere *= actorsHere.size
                        }

                        count+=counterHere
                    }
                }
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

class OldSRTParser(val input: InputStream, val name: String) {
    fun parse(): OldSRT {
        var result: OldSRT = OldSRT()
        result.subName = name
        val reader = BufferedReader(InputStreamReader(input))
        var currentDialog = OldSrtDialogue()
        var sectionBefore = ""


        while (true) {
            val line = reader.readLine() ?: break
            when {
                line.toIntOrNull() != null -> {
                    if (currentDialog.number != null) {
                        result.dialogs.add(currentDialog)
                        currentDialog = OldSrtDialogue()
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
                    } else {
                        if(currentDialog.dialog == null && line != "" ) {
                            currentDialog.dialog = line
                        }
                    }
                }
            }
        }
        result.dialogs.add(currentDialog)
        return result
    }
}


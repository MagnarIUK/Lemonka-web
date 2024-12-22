package com.magnariuk.lemonkasubstoolw.data.api.subs

import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.util.*


class ASSCounter(val ass: Ass) {
    fun count_dialogs(): Int {
        var result = 0
        ass.events.dialogues.forEach {
            result += it.actor.split("and", ",").size
        }
        return result
    }

    fun count(): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        var count = 0
        val numbers: MutableList<Int> = mutableListOf()

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


}
package com.magnariuk.lemonkasubstoolw.data.util

import com.github.mvysny.karibudsl.v10.onLeftClick
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant


fun countWordsInNumber(number: Int): Int {
    if (number == 0) return 1

    val units = listOf("", "один", "два", "три", "чотири", "п'ять", "шість", "сім", "вісім", "дев'ять")
    val teens = listOf("десять", "одинадцять", "дванадцять", "тринадцять", "чотирнадцять", "п'ятнадцять", "шістнадцять", "сімнадцять", "вісімнадцять", "дев'ятнадцять")
    val tens = listOf("", "", "двадцять", "тридцять", "сорок", "п'ятдесят", "шістдесят", "сімдесят", "вісімдесят", "дев'яносто")
    val hundreds = listOf("", "сто", "двісті", "триста", "чотириста", "п'ятсот", "шістсот", "сімсот", "вісімсот", "дев'ятсот")
    val thousands = listOf("", "тисяча", "тисячі", "тисяч")

    var num = number
    val words = mutableListOf<String>()

    if (num >= 1000) {
        val thousandsPart = num / 1000
        num %= 1000

        if (thousandsPart == 1) {
            words.add("тисяча")
        } else {
            if (thousandsPart % 10 == 1 && thousandsPart % 100 != 11) {
                words.add(convertNumberToWords(thousandsPart) + " " + "тисяча")
            } else if (thousandsPart % 10 in 2..4 && thousandsPart % 100 !in 12..14) {
                words.add(convertNumberToWords(thousandsPart) + " " + "тисячі")
            } else {
                words.add(convertNumberToWords(thousandsPart) + " " + "тисяч")
            }
        }
    }

    if (num > 0) {
        words.add(convertNumberToWords(num))
    }

    return words.joinToString(" ").split(" ").size
}

fun convertNumberToWords(number: Int): String {
    val units = listOf("", "один", "два", "три", "чотири", "п'ять", "шість", "сім", "вісім", "дев'ять")
    val teens = listOf("десять", "одинадцять", "дванадцять", "тринадцять", "чотирнадцять", "п'ятнадцять", "шістнадцять", "сімнадцять", "вісімнадцять", "дев'ятнадцять")
    val tens = listOf("", "", "двадцять", "тридцять", "сорок", "п'ятдесят", "шістдесят", "сімдесят", "вісімдесят", "дев'яносто")
    val hundreds = listOf("", "сто", "двісті", "триста", "чотириста", "п'ятсот", "шістсот", "сімсот", "вісімсот", "дев'ятсот")

    var num = number
    val words = mutableListOf<String>()

    if (num >= 100) {
        val hundredsPart = num / 100
        words.add(hundreds[hundredsPart])
        num %= 100
    }

    if (num in 10..19) {
        words.add(teens[num - 10])
    } else {
        if (num >= 20) {
            val tensPart = num / 10
            words.add(tens[tensPart])
            num %= 10
        }
        if (num > 0) {
            words.add(units[num])
        }
    }

    return words.joinToString(" ")
}

fun showError(message: String, dur: Int = 5000, showCloseButton: Boolean = false) {
    val notification = Notification(message)
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR)
    notification.position = (Notification.Position.TOP_CENTER)
    notification.duration = dur
    if(showCloseButton){
        notification.add(
            NativeLabel(message),
            Button().apply {
                icon = Icon(VaadinIcon.CLOSE)
                onLeftClick {
                    notification.close()
                }
            })
    }
    notification.open()
}

fun showWarning(message: String, dur: Int = 5000, showCloseButton: Boolean = false) {
    val notification = Notification(message)
    notification.addThemeVariants(NotificationVariant.LUMO_WARNING)
    notification.position = (Notification.Position.TOP_CENTER)
    notification.duration = dur
    if(showCloseButton){
        notification.add(
            NativeLabel(message),
            Button().apply {
                icon = Icon(VaadinIcon.CLOSE)
                onLeftClick {
                    notification.close()
                }
            })
    }
    notification.open()
}

fun showSuccess(message: String, dur: Int = 5000, showCloseButton: Boolean = false) {
    val notification = Notification(message)
    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS)
    notification.position = (Notification.Position.TOP_CENTER)
    notification.duration = dur
    if(showCloseButton){
        notification.add(
            NativeLabel(message),
            Button().apply {
                icon = Icon(VaadinIcon.CLOSE)
                onLeftClick {
                    notification.close()
                }
            })
    }
    notification.open()

}
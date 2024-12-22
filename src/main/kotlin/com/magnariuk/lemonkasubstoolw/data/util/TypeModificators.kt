package com.magnariuk.lemonkasubstoolw.data.util

/**
 * Повертає значення в пікселях, наприклад: 25.px -> "25px".
 */
val Int.px: String
    get() = "${this}px"

/**
 * Повертає значення в відсотках, наприклад: 25.px -> "25%".
 */
val Int.p: String
    get() = "${this}%"

val Int.s: String get() = "$this"

val Long.mb: String get() {
    val kb = 1024
    val mb = 1024 * kb
    val gb = 1024 * mb

    return when {
        this >= gb -> "%.2f GB".format(this / gb.toDouble())
        this >= mb -> "%.2f MB".format(this / mb.toDouble())
        this >= kb -> "%.2f KB".format(this / kb.toDouble())
        else -> "$this bytes"
    }
}

/**
 * Повертає значення в секундах, наприклад: 1.2.s -> "1.2s".
 */
val Double.s: String get() = "${this}s"

/**
 * Повертає значення в hex кольорі, наприклад: "d3d3d3".p -> "#d3d3d3".
 */
val String.hex: String get() = "#${this}"
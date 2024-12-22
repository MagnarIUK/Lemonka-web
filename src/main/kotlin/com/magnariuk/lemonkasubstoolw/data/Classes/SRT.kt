package com.magnariuk.lemonkasubstoolw.data.Classes

data class SRT(
    var subName: String? = null,
    var dialogs: MutableList<SrtDialogue> = mutableListOf(),
)

data class SrtDialogue(
    var number: Int? = null,
    var startTime: String? = null,
    var endTime: String? = null,
    var dialog: String? = null,
    var actors: MutableList<String> = mutableListOf(),
)
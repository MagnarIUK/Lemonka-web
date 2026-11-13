package com.magnariuk.lemonka.subs

data class SRT(
    var subName: String? = null,
    var dialogs: MutableList<SrtDialogue> = mutableListOf(),
)

data class SrtDialogue(
    var number: Int? = null,
    var startTime: String? = null,
    var endTime: String? = null,
    var dialog: String? = null,
    /** The `[...]` part before the text, unsplit; null if the line has no speaker. */
    var rawActors: String? = null,
)

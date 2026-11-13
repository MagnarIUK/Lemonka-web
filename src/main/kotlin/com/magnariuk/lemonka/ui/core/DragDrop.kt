package com.magnariuk.lemonka.ui.core

import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.HTMLTag

// Drag & drop from Kotlin. The browser part is the generic /static/js/dnd.js - nothing to write in JS.
//
//   span("chip") { dragItem("${character.id}"); +character.name }
//   div("row")   { dropZone("/tools/sub/1/drop", "actor" to actor.id) }
//
// On drop, the zone's URL gets a POST with `item` = the dragged value plus the extra values.
// Answer it like any HTMX request (out-of-band swaps, toasts).

/** Makes this element draggable; [value] is sent as `item` when it's dropped on a [dropZone]. */
fun HTMLTag.dragItem(value: String) {
    attributes["draggable"] = "true"
    attributes["data-drag"] = value
}

/** Makes this element accept [dragItem]s; a drop POSTs to [url] with `item` and [values]. */
fun HTMLTag.dropZone(url: String, vararg values: Pair<String, Any>) {
    attributes["data-drop"] = url
    if (values.isNotEmpty()) {
        attributes["data-drop-vals"] = values.joinToString(",", "{", "}") { (k, v) ->
            "\"$k\":" + if (v is Number || v is Boolean) "$v" else "\"${v.toString().replace("\"", "\\\"")}\""
        }
    }
}

/** Files dropped anywhere on this element go into the `<input type=file>` inside it. */
fun HTMLTag.uploadDropZone() {
    attributes["data-upload"] = ""
}

object DragDropStyle : Styled {
    override fun CssBuilder.styles() {
        rule("[data-drag]") { cursor = Cursor.grab; userSelect = UserSelect.none }
        rule("[data-drag]:active") { cursor = Cursor.grabbing }
        rule(".dragging") { opacity = 0.35; transform { scale(0.95) } }
        rule("[data-drop]") {
            put("transition", "background-color .15s, box-shadow .15s")
            borderRadius = LinearDimension(v("radius-s"))
        }
        // While something is being dragged, every place it can go gets a dashed outline.
        rule(".is-dragging [data-drop]") {
            put("box-shadow", "inset 0 0 0 1.5px ${v("primary-30")}")
            backgroundColor = tc("contrast-5")
        }
        rule("[data-drop].drag-over") {
            backgroundColor = tc("primary-10")
            put("box-shadow", "inset 0 0 0 2px ${v("primary")}")
        }
    }
}

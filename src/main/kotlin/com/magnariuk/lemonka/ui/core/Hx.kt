package com.magnariuk.lemonka.ui.core

import kotlinx.html.HTMLTag

// Typed HTMX attributes, so pages stay pure Kotlin. See https://htmx.org/reference/

fun HTMLTag.hxGet(url: String) { attributes["hx-get"] = url }
fun HTMLTag.hxPost(url: String) { attributes["hx-post"] = url }
fun HTMLTag.hxDelete(url: String) { attributes["hx-delete"] = url }
fun HTMLTag.hxTarget(selector: String) { attributes["hx-target"] = selector }
fun HTMLTag.hxSwap(swap: String) { attributes["hx-swap"] = swap }
fun HTMLTag.hxTrigger(trigger: String) { attributes["hx-trigger"] = trigger }
fun HTMLTag.hxSelect(selector: String) { attributes["hx-select"] = selector }
fun HTMLTag.hxInclude(selector: String) { attributes["hx-include"] = selector }
fun HTMLTag.hxPushUrl(value: String = "true") { attributes["hx-push-url"] = value }
fun HTMLTag.hxBoost(on: Boolean = true) { attributes["hx-boost"] = on.toString() }
fun HTMLTag.hxEncoding(value: String) { attributes["hx-encoding"] = value }
fun HTMLTag.hxSwapOob(value: String = "true") { attributes["hx-swap-oob"] = value }

/** Native browser confirmation before the request is sent (built into HTMX). */
fun HTMLTag.hxConfirm(question: String) { attributes["hx-confirm"] = question }

/** Extra values sent with the request, e.g. `hxVals("id" to 5)`. */
fun HTMLTag.hxVals(vararg pairs: Pair<String, Any>) {
    attributes["hx-vals"] = pairs.joinToString(",", "{", "}") { (k, value) ->
        "\"$k\":" + if (value is Number || value is Boolean) "$value" else "\"${value.toString().replace("\"", "\\\"")}\""
    }
}

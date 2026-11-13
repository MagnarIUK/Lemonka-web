package com.magnariuk.lemonka.web

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*

/** True when the request was sent by HTMX (as opposed to a normal page load). */
val ApplicationCall.isHtmx: Boolean get() = request.header("HX-Request") == "true"

/** Redirects a normal request with 303, or tells HTMX to do a full-page navigation. */
suspend fun ApplicationCall.hxRedirect(url: String) {
    if (isHtmx) {
        response.header("HX-Redirect", url)
        respond(HttpStatusCode.NoContent)
    } else {
        respondRedirect(url, permanent = false)
    }
}

/** Only allow redirects to paths on this site, never to another host. */
fun safeNext(next: String?): String =
    next?.takeIf { it.startsWith("/") && !it.startsWith("//") } ?: "/"

/**
 * The site's public address, used for invite links. Set `PUBLIC_URL` (e.g. https://tools.example.com)
 * when running behind a reverse proxy; otherwise it is taken from the request.
 */
fun ApplicationCall.baseUrl(): String {
    application.environment.config.propertyOrNull("lemonka.publicUrl")?.getString()
        ?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }?.let { return it }
    val o = request.origin
    val defaultPort = (o.scheme == "http" && o.serverPort == 80) || (o.scheme == "https" && o.serverPort == 443)
    return "${o.scheme}://${o.serverHost}${if (defaultPort) "" else ":${o.serverPort}"}"
}

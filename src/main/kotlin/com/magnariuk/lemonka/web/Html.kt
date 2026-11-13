package com.magnariuk.lemonka.web

import com.magnariuk.lemonka.data.db.User
import com.magnariuk.lemonka.ui.components.Toast
import com.magnariuk.lemonka.ui.components.ToastTone
import com.magnariuk.lemonka.ui.layout.AppLayout
import com.magnariuk.lemonka.ui.layout.Shell
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Renders a full page inside the app shell. */
suspend fun ApplicationCall.respondPage(
    title: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    content: MAIN.() -> Unit,
) {
    val shell = Shell(
        user = principal<User>(),
        path = request.path(),
        nav = Features.all.mapNotNull { it.nav },
        menu = Features.all.mapNotNull { it.menu },
    )
    respondHtml(status) { AppLayout(title, shell, content) }
}

/** Renders loose HTML (no `<html>`, no wrapper tag) - what HTMX swaps into the page. */
suspend fun ApplicationCall.respondFragment(
    status: HttpStatusCode = HttpStatusCode.OK,
    content: FlowContent.() -> Unit,
) = respondText(fragment(content), ContentType.Text.Html.withCharset(Charsets.UTF_8), status)

/** Builds an HTML fragment as a string. The helper tag around it is not emitted. */
fun fragment(content: FlowContent.() -> Unit): String {
    val out = createHTML(prettyPrint = false)
    val skipRoot = object : TagConsumer<String> by out {
        override fun onTagStart(tag: Tag) { if (tag !is FragmentRoot) out.onTagStart(tag) }
        override fun onTagEnd(tag: Tag) { if (tag !is FragmentRoot) out.onTagEnd(tag) }
    }
    FragmentRoot(skipRoot).visit(content)
    return out.finalize()
}

private class FragmentRoot(consumer: TagConsumer<*>) :
    HTMLTag("fragment", consumer, emptyMap(), null, false, false), HtmlBlockTag

/**
 * Answers an HTMX request with only a toast and tells HTMX not to swap anything else.
 * Handy for actions like "saved" or for errors.
 */
suspend fun ApplicationCall.respondToast(message: String, tone: ToastTone = ToastTone.Success) {
    response.header("HX-Reswap", "none")
    respondFragment { Toast(message, tone) }
}

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

/** Unix seconds → "13.11.2026", or "-" if unknown. */
fun formatDate(seconds: Long?): String = seconds?.let { dateFormat.format(Instant.ofEpochSecond(it)) } ?: "-"

/** Ukrainian plural: plural(1, "репліка", "репліки", "реплік") → "1 репліка"; 3 → "3 репліки"; 5 → "5 реплік". */
fun plural(n: Int, one: String, few: String, many: String): String {
    val word = when {
        n % 100 in 11..14 -> many
        n % 10 == 1 -> one
        n % 10 in 2..4 -> few
        else -> many
    }
    return "$n $word"
}

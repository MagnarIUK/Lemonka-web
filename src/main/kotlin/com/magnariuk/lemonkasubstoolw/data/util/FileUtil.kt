package com.magnariuk.lemonkasubstoolw.data.util

import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets


fun write_to_txt(txt: String): StreamResource {
    return generateStreamResourceFromString("Пораховані субтитри.txt", txt)
}

fun generateStreamResourceFromString(filename: String, lines: String): StreamResource {
    return StreamResource(filename, InputStreamFactory { ByteArrayInputStream(lines.toByteArray(StandardCharsets.UTF_8)) })
}

fun generateStreamResource(filename: String, lines: MutableList<String>): StreamResource {
    val content = lines.joinToString(separator = System.lineSeparator())
    return StreamResource(filename, InputStreamFactory { ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8)) })
}

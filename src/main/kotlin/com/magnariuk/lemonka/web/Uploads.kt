package com.magnariuk.lemonka.web

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.utils.io.jvm.javaio.*

class UploadedFile(val name: String, val bytes: ByteArray) {
    val extension get() = name.substringAfterLast('.', "").lowercase()
}

/** A multipart form: uploaded files plus the ordinary fields (a field can repeat, e.g. checkboxes). */
class UploadForm(val files: List<UploadedFile>, val fields: Map<String, List<String>>) {
    operator fun get(name: String): String? = fields[name]?.firstOrNull()
    fun all(name: String): List<String> = fields[name].orEmpty()
}

/** Reads a multipart (file upload) form fully into memory. Files with no name (nothing picked) are skipped. */
suspend fun ApplicationCall.receiveUploadForm(maxBytes: Long): UploadForm {
    val files = mutableListOf<UploadedFile>()
    val fields = mutableMapOf<String, MutableList<String>>()
    receiveMultipart(formFieldLimit = maxBytes).forEachPart { part ->
        when (part) {
            is PartData.FileItem -> part.originalFileName?.takeIf { it.isNotBlank() }?.let { name ->
                files += UploadedFile(name, part.provider().toInputStream().use { it.readBytes() })
            }
            is PartData.FormItem -> fields.getOrPut(part.name.orEmpty()) { mutableListOf() } += part.value
            else -> {}
        }
        part.release()
    }
    return UploadForm(files, fields)
}

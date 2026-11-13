package com.magnariuk.lemonka.ui.components

import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.core.uploadDropZone
import com.magnariuk.lemonka.ui.core.tc
import com.magnariuk.lemonka.ui.core.v
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/**
 * File picker drawn as a drop zone (replaces Vaadin's Upload). Files can be dropped anywhere on it
 * (handled by /static/js/dnd.js via [uploadDropZone]) or picked; the browser shows the chosen names.
 * Must be inside a form with `hx-encoding="multipart/form-data"`.
 */
fun FlowContent.Upload(
    name: String,
    accept: String,
    hint: String,
    multiple: Boolean = true,
    required: Boolean = true,
) = label("upload") {
    uploadDropZone()
    span("upload-icon") { Icon(Icon.Upload) }
    span("upload-title") { +if (multiple) "Перетягніть файли сюди або оберіть" else "Перетягніть файл сюди або оберіть" }
    span("muted small") { +hint }
    input(InputType.file, name = name, classes = "upload-input") {
        this.accept = accept
        this.multiple = multiple
        this.required = required
    }
}

object UploadStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".upload") {
            position = Position.relative
            display = Display.flex; flexDirection = FlexDirection.column; alignItems = Align.center; gap = 4.px
            padding = Padding(22.px, 16.px, 16.px)
            put("border", "2px dashed ${v("contrast-20")}"); borderRadius = LinearDimension(v("radius-m"))
            backgroundColor = tc("contrast-5"); textAlign = TextAlign.center; cursor = Cursor.pointer
            put("transition", "border-color .15s, background-color .15s")
        }
        rule(".upload:hover, .upload:focus-within, .upload.drag-over") { borderColor = tc("primary"); backgroundColor = tc("primary-10") }
        rule(".upload.drag-over") { put("box-shadow", "0 0 0 4px ${v("primary-10")}"); transform { scale(1.01) } }
        rule(".upload-icon") {
            display = Display.flex; width = 44.px; height = 44.px; borderRadius = 50.pct
            alignItems = Align.center; justifyContent = JustifyContent.center
            backgroundColor = tc("base"); color = tc("primary"); fontSize = 20.px; put("box-shadow", v("shadow-s"))
        }
        rule(".upload-title") { fontWeight = FontWeight.w500 }
        rule(".upload-input") { marginTop = 10.px; maxWidth = 100.pct; fontSize = 0.875.rem; color = tc("text-secondary") }
        rule(".upload-input::file-selector-button") {
            put("font", "inherit"); fontWeight = FontWeight.w500; marginRight = 10.px
            padding = Padding(6.px, 12.px); border = Border.none; borderRadius = LinearDimension(v("radius-s"))
            backgroundColor = tc("primary"); color = Color.white; cursor = Cursor.pointer
        }
    }
}

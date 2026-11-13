package com.magnariuk.lemonka.features.sheet

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Checks an uploaded picture and scales it down so the sheet loads fast. */
object Images {
    const val MAX_UPLOAD = 15L * 1024 * 1024
    private const val MAX_SIDE = 800
    private const val MAX_PASSTHROUGH = 3 * 1024 * 1024

    sealed interface Result {
        data class Ok(val contentType: String, val bytes: ByteArray) : Result
        data class Error(val message: String) : Result
    }

    fun process(bytes: ByteArray, filename: String): Result {
        if (bytes.isEmpty()) return Result.Error("Порожній файл")
        val image = runCatching { ImageIO.read(bytes.inputStream()) }.getOrNull()
        if (image == null) {
            // Java can't decode WebP; keep those as they are if they're a sensible size.
            val isWebp = bytes.size > 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WEBP"
            return when {
                !isWebp -> Result.Error("Це не схоже на картинку (підтримуються JPG, PNG, GIF, WebP)")
                bytes.size > MAX_PASSTHROUGH -> Result.Error("WebP більше 3 МБ - збережіть як JPG або PNG")
                else -> Result.Ok("image/webp", bytes)
            }
        }
        val gif = filename.endsWith(".gif", ignoreCase = true)
        if (gif && bytes.size <= MAX_PASSTHROUGH) return Result.Ok("image/gif", bytes) // keep animation
        val scaled = scale(image)
        val alpha = scaled.colorModel.hasAlpha()
        val out = ByteArrayOutputStream()
        ImageIO.write(if (alpha) scaled else rgb(scaled), if (alpha) "png" else "jpg", out)
        return Result.Ok(if (alpha) "image/png" else "image/jpeg", out.toByteArray())
    }

    private fun scale(src: BufferedImage): BufferedImage {
        val ratio = MAX_SIDE.toDouble() / maxOf(src.width, src.height)
        if (ratio >= 1.0) return src
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val type = if (src.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        return BufferedImage(w, h, type).also { dst ->
            dst.createGraphics().apply {
                setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                drawImage(src, 0, 0, w, h, null)
                dispose()
            }
        }
    }

    private fun rgb(src: BufferedImage): BufferedImage {
        if (src.type == BufferedImage.TYPE_INT_RGB) return src
        return BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB).also {
            it.createGraphics().apply { drawImage(src, 0, 0, java.awt.Color.WHITE, null); dispose() }
        }
    }
}

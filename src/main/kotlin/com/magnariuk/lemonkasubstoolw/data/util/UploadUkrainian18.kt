package com.magnariuk.lemonkasubstoolw.data.util

import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.UploadI18N

class UploadUkrainian18: UploadI18N() {
    init {
        this.setUnits(Units(mutableListOf("Б", "Кб", "Мб", "Гб", "Тб", "Пб", "Еб","Зтб", "Йб")))

        val uploadingStatus = Uploading.Status().apply {
            connecting = "Підключення"
            held = "Утримується"
            processing = "Обробка"
            stalled = "Зупинено"
        }
        val uploadingTime = Uploading.RemainingTime().apply {
            unknown = "Невідомо"
            prefix = "лишилося: "
        }

        val uploadingError = Uploading.Error().apply {
            forbidden = "Заборонено"
            serverUnavailable = "Сервер недоступний"
            unexpectedServerError = "Невідома помилка серверу"
        }

        val uploading = Uploading().apply {
            status = uploadingStatus
            remainingTime = uploadingTime
            error = uploadingError
        }

        val addFiles = AddFiles().apply {
            many = "Додайте файли..."
            one = "Додайте файл..."
        }
        val dropFiles = DropFiles().apply {
            many = "Киньте файли тут"
            one = "Киньте файл тут"
        }

        val error = Error().apply {
            fileIsTooBig = "Файл завеликий"
            incorrectFileType = "Невірний тип файлу"
            tooManyFiles = "Забагато файлів"
        }

        val file = File().apply {
            remove = "Прибрати"
            retry = "Спробувати ще раз"
            start = "Почати"
        }




        this.setFile(file)
        this.setError(error)
        this.setDropFiles(dropFiles)
        this.setAddFiles(addFiles)
        this.setUploading(uploading)
    }
}
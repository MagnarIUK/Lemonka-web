package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.magnariuk.lemonkasubstoolw.data.api.database.AuthService
import com.magnariuk.lemonkasubstoolw.data.util.px
import com.magnariuk.lemonkasubstoolw.data.util.showSuccess
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField

fun showLoginDialog(authService: AuthService, onSuccess: () -> Unit) {
    Dialog().apply {
        isCloseOnOutsideClick = false
        isCloseOnEsc = false

        val usernameField = TextField("Ім'я користувача").apply { width = 300.px; isRequired = true }
        val passwordField = PasswordField("Пароль").apply { width = 300.px; isRequired = true }

        val logIn = Button("Увійти").apply {
            setWidth(200.px)
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            addClickListener {
                when (authService.login(usernameField.value.trim(), passwordField.value.trim())) {
                    "s" -> { showSuccess("Успішний вхід"); close(); onSuccess() }
                    "e:pnv" -> { passwordField.isInvalid = true; passwordField.errorMessage = "невірний пароль" }
                    "e:unf" -> { usernameField.isInvalid = true; usernameField.errorMessage = "користувача не знайдено" }
                }
            }
        }
        val signUp = Button("Зареєструватися").apply {
            setWidth(200.px)
            addClickListener {
                val u = usernameField.value.trim(); val p = passwordField.value.trim()
                when {
                    u.length < 4 -> { usernameField.isInvalid = true; usernameField.errorMessage = "ім'я користувача занадто коротке" }
                    p.length < 8 -> { passwordField.isInvalid = true; passwordField.errorMessage = "пароль занадто короткий" }
                    else -> when (authService.register(u, p)) {
                        "e:uax" -> { usernameField.isInvalid = true; usernameField.errorMessage = "користувач з таким ім'ям вже існує" }
                        "s" -> { showSuccess("Успішна реєстрація"); close(); onSuccess() }
                    }
                }
            }
        }
        add(VerticalLayout(usernameField, passwordField, logIn, signUp).apply {
            alignItems = FlexComponent.Alignment.CENTER
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        })
        open()
    }
}
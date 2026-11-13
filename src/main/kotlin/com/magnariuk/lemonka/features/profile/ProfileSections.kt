package com.magnariuk.lemonka.features.profile

import com.magnariuk.lemonka.auth.AuthResult.Field
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import com.magnariuk.lemonka.web.formatDate
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

// Each section is its own composable with a stable id, so routes can re-render just that part.

fun FlowContent.ProfileHeader(user: User, inviter: String?) = div("profile-header") {
    span("avatar avatar-l") { +user.username.take(1).uppercase() }
    div {
        Row(gap = 8) {
            h1 { +user.username }
            Badge(user.role.title, if (user.isAdmin) BadgeTone.Primary else BadgeTone.Neutral)
        }
        div("muted") {
            +"Акаунт з ${formatDate(user.createdAt)}"
            if (inviter != null) +" · запрошення від $inviter"
        }
    }
}

data class PasswordErrors(val field: Field, val message: String)

fun FlowContent.PasswordCard(error: PasswordErrors? = null) = Card("Змінити пароль", id = "password-card") {
    form {
        hxPost("/profile/password"); hxTarget("#password-card"); hxSwap("outerHTML")
        Column(gap = 12) {
            TextField("Поточний пароль", "current", type = InputType.password, required = true,
                error = error?.takeIf { it.field == Field.Password }?.message) { attributes["autocomplete"] = "current-password" }
            TextField("Новий пароль", "new", type = InputType.password, required = true,
                error = error?.takeIf { it.field == Field.NewPassword }?.message) { attributes["autocomplete"] = "new-password" }
            TextField("Повторіть новий пароль", "repeat", type = InputType.password, required = true,
                error = error?.takeIf { it.field == Field.Repeat }?.message) { attributes["autocomplete"] = "new-password" }
            Row { Button("Зберегти пароль", ButtonVariant.Primary, type = ButtonType.submit) }
        }
    }
}

fun FlowContent.SettingsCard(settings: Setting, sessions: Long) = Card("Налаштування") {
    Column(gap = 18) {
        Switch("Приховувати вже призначених акторів у списку", "hideSelected", settings.hideSelected) {
            hxPost("/profile/settings"); hxTrigger("change")
        }
        Row(gap = 10) {
            div {
                div { +"Активні сесії: $sessions" }
                div("muted small") { +"Кожен вхід з іншого браузера чи пристрою - окрема сесія." }
            }
            Spacer()
            Button("Вийти на інших пристроях", small = true, icon = Icon.Logout) {
                hxPost("/profile/sessions/logout-others")
                hxConfirm("Вийти з усіх інших сесій?")
            }
        }
    }
}

fun FlowContent.DangerCard() = Card("Небезпечна зона", classes = "danger-card") {
    Row(gap = 12) {
        div("muted") { +"Видалення акаунта прибере всі ваші проєкти, акторів і персонажів назавжди." }
        Spacer()
        Button("Видалити акаунт", ButtonVariant.Danger, icon = Icon.Trash) {
            hxGet("/profile/delete"); hxTarget("#modal")
        }
    }
}

fun FlowContent.DeleteAccountDialog(error: String? = null) = Dialog("Видалити акаунт?") {
    form {
        hxPost("/profile/delete"); hxTarget("closest .modal"); hxSwap("outerHTML")
        Column(gap = 14) {
            p { +"Цю дію не можна скасувати. Введіть пароль, щоб підтвердити." }
            TextField("Пароль", "password", type = InputType.password, required = true, autofocus = true, error = error)
            Row(gap = 8, arrange = Arrangement.End) {
                DialogCloseButton()
                Button("Видалити назавжди", ButtonVariant.Danger, type = ButtonType.submit)
            }
        }
    }
}

fun FlowContent.ProjectsCard(projects: List<ProjectOverview>, oob: Boolean = false) = Card("Мої проєкти", id = "projects-card", attrs = { if (oob) hxSwapOob("outerHTML") }) {
    if (projects.isEmpty()) {
        EmptyState("Проєктів ще немає - вони з'являються, коли ви розділяєте субтитри", Icon.Subtitles)
        return@Card
    }
    Table(listOf("Назва", "Персонажі", "Актори", "")) {
        projects.forEach { (p, chars, actors) ->
            tr {
                td { b { +p.name } }
                td { +"$chars" }
                td { +"$actors" }
                td("actions") {
                    Button("Персонажі", small = true, variant = ButtonVariant.Tertiary, icon = Icon.Edit) {
                        hxGet("/profile/projects/${p.id}"); hxTarget("#modal")
                    }
                    Button(small = true, variant = ButtonVariant.Danger, icon = Icon.Trash) {
                        attributes["aria-label"] = "Видалити проєкт"
                        hxDelete("/profile/projects/${p.id}")
                        hxConfirm("Видалити проєкт «${p.name}» разом з усіма персонажами?")
                        hxTarget("closest tr"); hxSwap("outerHTML swap:250ms")
                    }
                }
            }
        }
    }
}

fun FlowContent.ProjectDialog(project: Project, characters: List<Character>, actorNames: Map<Int, String>) =
    Dialog(project.name, wide = true) {
        Column(gap = 18) {
            form {
                hxPost("/profile/projects/${project.id}/rename"); hxTarget("closest .modal"); hxSwap("delete")
                Row(gap = 8, align = Alignment.End) {
                    div("grow") { TextField("Назва проєкту", "name", project.name, required = true) }
                    Button("Перейменувати", type = ButtonType.submit)
                }
            }
            if (characters.isEmpty()) EmptyState("У проєкті немає персонажів")
            else Table(listOf("Персонаж", "Актор", "")) {
                characters.sortedBy { it.name.lowercase() }.forEach { c ->
                    tr {
                        td { +c.name }
                        td { c.actor?.let { +(actorNames[it] ?: "-") } ?: span("muted") { +"не призначено" } }
                        td("actions") {
                            Button(small = true, variant = ButtonVariant.Danger, icon = Icon.Trash) {
                                attributes["aria-label"] = "Видалити персонажа"
                                hxDelete("/profile/characters/${c.id}")
                                hxTarget("closest tr"); hxSwap("outerHTML swap:250ms")
                            }
                        }
                    }
                }
            }
        }
    }

fun FlowContent.ActorsCard(actors: List<Pair<Actor, Long>>, error: String? = null) = Card("Мої актори", id = "actors-card") {
    form(classes = "actor-add") {
        hxPost("/profile/actors"); hxTarget("#actors-card"); hxSwap("outerHTML")
        Row(gap = 8, align = Alignment.Start) {
            div("grow") { TextField(null, "name", placeholder = "Ім'я нового актора", required = true, error = error) }
            Button("Додати", ButtonVariant.Primary, icon = Icon.Plus, type = ButtonType.submit)
        }
    }
    if (actors.isEmpty()) {
        EmptyState("Акторів ще немає", Icon.User)
        return@Card
    }
    Table(listOf("Актор", "Проєктів", "")) {
        actors.forEach { (a, projects) ->
            tr {
                td { +a.name }
                td { +"$projects" }
                td("actions") {
                    Button(small = true, variant = ButtonVariant.Danger, icon = Icon.Trash) {
                        attributes["aria-label"] = "Видалити актора"
                        hxDelete("/profile/actors/${a.id}")
                        hxConfirm("Видалити актора «${a.name}»? Його персонажі стануть непризначеними.")
                        hxTarget("closest tr"); hxSwap("outerHTML swap:250ms")
                    }
                }
            }
        }
    }
}

object ProfileStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".profile-header") { display = Display.flex; alignItems = Align.center; gap = 16.px; marginBottom = 24.px }
        rule(".avatar-l") { width = 64.px; height = 64.px; fontSize = 1.6.rem; put("box-shadow", "0 8px 20px ${v("primary-30")}") }
        rule(".profile-grid") {
            display = Display.grid; gap = 16.px; alignItems = Align.start
            put("grid-template-columns", "minmax(0, 1fr) minmax(0, 1.4fr)")
        }
        rule(".profile-grid > .col > .card:nth-child(2)") { put("animation-delay", ".04s") }
        rule(".profile-grid > .col > .card:nth-child(3)") { put("animation-delay", ".08s") }
        rule(".grow") { flexGrow = 1.0; minWidth = 0.px }
        rule(".small") { fontSize = 0.8125.rem }
        rule(".actor-add") { marginBottom = 12.px }
        rule(".danger-card") { put("border", "1px solid ${v("error-10")}") }
        rule(".danger-card h3") { color = tc("error") }
        media("(max-width: 860px)") {
            rule(".profile-grid") { put("grid-template-columns", "1fr") }
        }
    }
}

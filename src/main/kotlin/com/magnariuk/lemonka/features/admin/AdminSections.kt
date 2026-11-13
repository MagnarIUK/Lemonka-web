package com.magnariuk.lemonka.features.admin

import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import com.magnariuk.lemonka.web.formatDate
import com.magnariuk.lemonka.web.plural
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

enum class AdminTab(val title: String, val path: String, val icon: Icon) {
    Invites("Запрошення", "/admin/invites", Icon.Plus),
    Users("Користувачі", "/admin/users", Icon.User),
    Separators("Роздільники", "/admin/separators", Icon.Edit),
}

fun FlowContent.AdminTabs(active: AdminTab) = div("admin-head") {
    Row(gap = 10) {
        span("tile-icon") { Icon(Icon.Shield) }
        div {
            h1 { +"Адміністрування" }
            div("muted") { +"Запрошення, користувачі та налаштування для всіх" }
        }
    }
    nav("tabs") {
        AdminTab.entries.forEach { t ->
            a(href = t.path, classes = "tab${if (t == active) " active" else ""}") { Icon(t.icon); span { +t.title } }
        }
    }
}

// ---------------- Invites ----------------

fun FlowContent.CreateInviteCard() = Card("Нове запрошення") {
    form {
        hxPost("/admin/invites"); hxTarget("#invites-card"); hxSwap("outerHTML")
        div("invite-form") {
            TextField("Кількість використань", "maxUses", "1", type = InputType.number, required = true) {
                min = "1"; max = "100"
            }
            Select("Діє", "validDays", listOf(
                Option("1", "1 день"), Option("7", "7 днів"), Option("30", "30 днів"), Option("0", "Без обмежень"),
            ), selected = "7")
            TextField("Примітка (для кого)", "note", placeholder = "необов'язково") { maxLength = "200" }
            div("invite-submit") { Button("Створити", ButtonVariant.Primary, icon = Icon.Plus, type = ButtonType.submit) }
        }
    }
}

enum class InviteStatus(val title: String, val tone: BadgeTone) {
    Active("Активне", BadgeTone.Success), Used("Використане", BadgeTone.Neutral),
    Expired("Прострочене", BadgeTone.Neutral), Revoked("Відкликане", BadgeTone.Danger),
}

fun Invite.status(now: Long = nowSeconds()) = when {
    revoked -> InviteStatus.Revoked
    uses >= maxUses -> InviteStatus.Used
    expiresAt != null && expiresAt <= now -> InviteStatus.Expired
    else -> InviteStatus.Active
}

fun FlowContent.InvitesCard(
    invites: List<Invite>,
    creators: Map<Int, String>,
    usedBy: Map<Int, List<String>>,
    baseUrl: String,
    highlight: Int? = null,
) = Card("Усі запрошення", id = "invites-card") {
    if (invites.isEmpty()) {
        EmptyState("Запрошень ще немає. Створіть перше вище.", Icon.Plus)
        return@Card
    }
    Table(listOf("Код", "Статус", "Використано", "Діє до", "Створив", "")) {
        invites.forEach { inv ->
            val status = inv.status()
            tr(if (inv.id == highlight) "row-new" else null) {
                td {
                    div("code") { +inv.code }
                    inv.note?.let { div("muted small") { +it } }
                    if (status == InviteStatus.Active) {
                        input(InputType.text, classes = "input link-input") {
                            readonly = true
                            value = "$baseUrl/login?mode=register&invite=${inv.code}"
                            attributes["aria-label"] = "Посилання для реєстрації"
                        }
                    }
                }
                td { Badge(status.title, status.tone) }
                td {
                    +"${inv.uses} / ${inv.maxUses}"
                    usedBy[inv.id]?.let { div("muted small") { +it.joinToString(", ") } }
                }
                td { +(inv.expiresAt?.let { formatDate(it) } ?: "назавжди") }
                td { +(creators[inv.createdBy] ?: "-") }
                td("actions") {
                    if (status == InviteStatus.Active) {
                        Button("Відкликати", small = true, variant = ButtonVariant.Tertiary) {
                            hxPost("/admin/invites/${inv.id}/revoke"); hxTarget("#invites-card"); hxSwap("outerHTML")
                        }
                    }
                    Button(small = true, variant = ButtonVariant.Danger, icon = Icon.Trash) {
                        attributes["aria-label"] = "Видалити запрошення"
                        hxDelete("/admin/invites/${inv.id}")
                        hxConfirm("Видалити запрошення ${inv.code}?")
                        hxTarget("closest tr"); hxSwap("outerHTML swap:250ms")
                    }
                }
            }
        }
    }
}

// ---------------- Users ----------------

fun FlowContent.UsersCard(users: List<UserOverview>, me: User, oob: Boolean = false) = Card(
    "Користувачі (${users.size})", id = "users-card",
    attrs = { if (oob) hxSwapOob("outerHTML") },
    actions = {
        if (users.size > 1) Button("Об'єднати акаунти", small = true, icon = Icon.User) {
            hxGet("/admin/users/merge"); hxTarget("#modal")
        }
    },
) {
    Table(listOf("Користувач", "Роль", "Зареєстрований", "Запросив", "Проєкти", "Сесії", "")) {
        users.forEach { (u, inviter, projects, sessions) ->
            tr {
                td {
                    Row(gap = 10) {
                        span("avatar") { +u.username.take(1).uppercase() }
                        b { +u.username }
                        if (u.id == me.id) Badge("це ви", BadgeTone.Neutral)
                    }
                }
                td {
                    Select(null, "role", Role.entries.map { Option(it.id, it.title) }, selected = u.role.id) {
                        hxPost("/admin/users/${u.id}/role"); hxTrigger("change"); hxTarget("#users-card"); hxSwap("outerHTML")
                    }
                }
                td { +formatDate(u.createdAt) }
                td { +(inviter ?: "-") }
                td { +"$projects" }
                td { +"$sessions" }
                td("actions") {
                    Button("Скинути пароль", small = true, variant = ButtonVariant.Tertiary) {
                        hxPost("/admin/users/${u.id}/reset-password"); hxTarget("#modal")
                        hxConfirm("Згенерувати новий пароль для ${u.username}? Старий перестане працювати.")
                    }
                    if (u.id != me.id) Button(small = true, variant = ButtonVariant.Danger, icon = Icon.Trash) {
                        attributes["aria-label"] = "Видалити користувача"
                        hxDelete("/admin/users/${u.id}")
                        hxConfirm("Видалити ${u.username} разом з усіма проєктами й акторами?")
                        hxTarget("#users-card"); hxSwap("outerHTML")
                    }
                }
            }
        }
    }
}

fun FlowContent.MergeDialog(users: List<UserOverview>, me: User, error: String? = null) = Dialog("Об'єднати акаунти") {
    form {
        hxPost("/admin/users/merge"); hxTarget("closest .modal"); hxSwap("outerHTML")
        Column(gap = 16) {
            p("muted") {
                +"Усі проєкти, актори й персонажі з позначених акаунтів перейдуть в акаунт, який залишається. "
                +"Позначені акаунти буде видалено; їхні паролі перестануть працювати."
            }
            Select("Залишити акаунт", "target", users.map { Option("${it.user.id}", it.user.username) }, selected = "${me.id}", required = true)
            div("field") {
                span("field-label") { +"Приєднати до нього" }
                div("char-scroll") {
                    users.filter { it.user.id != me.id }.forEach { u ->
                        label("check") {
                            input(InputType.checkBox, name = "source") { value = "${u.user.id}" }
                            span("grow") { +u.user.username }
                            span("muted small") { +plural(u.projects.toInt(), "проєкт", "проєкти", "проєктів") }
                            if (u.user.isAdmin) Badge("адмін", BadgeTone.Primary)
                        }
                    }
                }
                span("muted small") { +"Ваш поточний акаунт (${me.username}) можна лише залишити, не приєднати." }
            }
            if (error != null) div("field-error") { Icon(Icon.Alert); +error }
            ul("merge-rules muted small") {
                li { +"Проєкти з однаковою назвою не зливаються - до назви додається ім'я старого акаунта." }
                li { +"Актори з однаковим ім'ям стають одним актором." }
                li { +"Якщо хтось із приєднаних був адміністратором, акаунт, що залишається, теж ним стане." }
            }
            Row(gap = 8, arrange = Arrangement.End) {
                DialogCloseButton()
                Button("Об'єднати", ButtonVariant.Danger, type = ButtonType.submit) {
                    hxConfirm("Об'єднати акаунти? Це не можна скасувати.")
                }
            }
        }
    }
}

fun FlowContent.TempPasswordDialog(username: String, password: String) = Dialog("Новий пароль для $username") {
    Column(gap = 14) {
        p { +"Передайте цей пароль користувачу. Він більше не буде показаний; користувач може змінити його у профілі." }
        div("code code-big") { +password }
        Row(arrange = Arrangement.End) { DialogCloseButton("Готово") }
    }
}

// ---------------- Separators ----------------

fun FlowContent.SeparatorsCard(separators: List<Separator>, error: String? = null) =
    Card("Роздільники імен персонажів", id = "separators-card") {
        p("muted") {
            +"Якщо в полі «актор» субтитрів кілька персонажів, їх розділяють цими словами чи символами - "
            +"наприклад «Fern та Stark». Діють для всіх користувачів."
        }
        div("chips") {
            if (separators.isEmpty()) span("muted") { +"Роздільників немає" }
            separators.forEach { s ->
                span("chip") {
                    span("code") { +"«${s.separator}»" }
                    button(classes = "chip-x") {
                        attributes["aria-label"] = "Видалити роздільник"
                        hxDelete("/admin/separators/${s.id}"); hxTarget("#separators-card"); hxSwap("outerHTML")
                        Icon(Icon.Close)
                    }
                }
            }
        }
        form {
            hxPost("/admin/separators"); hxTarget("#separators-card"); hxSwap("outerHTML")
            Row(gap = 8, align = Alignment.Start) {
                div("grow") { TextField(null, "separator", placeholder = "Новий роздільник, напр. «&» або «і»", required = true, error = error) }
                Button("Додати", ButtonVariant.Primary, icon = Icon.Plus, type = ButtonType.submit)
            }
        }
    }

object AdminStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".admin-head") { marginBottom = 20.px }
        rule(".admin-head h1") { fontSize = 1.5.rem }
        rule(".tabs") {
            display = Display.flex; gap = 4.px; marginTop = 18.px
            put("border-bottom", "1px solid ${v("contrast-10")}")
        }
        rule(".tab") {
            display = Display.flex; alignItems = Align.center; gap = 6.px
            padding = Padding(10.px, 14.px); color = tc("text-secondary"); fontWeight = FontWeight.w500
            put("border-bottom", "2px solid transparent"); marginBottom = (-1).px
            put("transition", "color .15s, border-color .2s")
        }
        rule(".tab:hover") { color = tc("text"); textDecoration = TextDecoration.none }
        rule(".tab.active") { color = tc("primary-text"); put("border-bottom-color", v("primary")) }
        rule(".admin-stack") { display = Display.flex; flexDirection = FlexDirection.column; gap = 16.px }

        rule(".invite-form") {
            display = Display.grid; gap = 12.px; alignItems = Align.end
            put("grid-template-columns", "minmax(140px, 1fr) minmax(140px, 1fr) minmax(200px, 2fr) auto")
        }
        media("(max-width: 860px)") { rule(".invite-form") { put("grid-template-columns", "1fr") } }

        rule(".code") {
            put("font-family", "ui-monospace, 'JetBrains Mono', Consolas, monospace")
            fontWeight = FontWeight.w600; letterSpacing = 0.04.em
        }
        rule(".code-big") {
            fontSize = 1.5.rem; textAlign = TextAlign.center; padding = Padding(14.px)
            backgroundColor = tc("primary-10"); color = tc("primary-text"); borderRadius = LinearDimension(v("radius-m"))
            put("user-select", "all")
        }
        rule(".link-input") { height = 28.px; fontSize = 0.75.rem; marginTop = 6.px; minWidth = 260.px; put("user-select", "all") }
        rule(".row-new") { put("animation", "row-flash 1.6s ${v("ease")}") }
        rule("@keyframes row-flash") { rule("from") { backgroundColor = tc("primary-30") } }
        rule(".table .select") { height = 32.px; width = LinearDimension.auto }

        rule(".merge-rules") { margin = Margin(0.px); paddingLeft = 18.px }
        rule(".chips") { display = Display.flex; flexWrap = FlexWrap.wrap; gap = 8.px; margin = Margin(14.px, 0.px) }
        rule(".chip") {
            display = Display.inlineFlex; alignItems = Align.center; gap = 4.px
            padding = Padding(4.px, 4.px, 4.px, 12.px); borderRadius = 999.px
            backgroundColor = tc("primary-10"); color = tc("primary-text")
            put("animation", "pop-in .2s ${v("ease")}")
        }
        rule(".chip-x") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center
            width = 22.px; height = 22.px; borderRadius = 50.pct; border = Border.none; cursor = Cursor.pointer
            backgroundColor = Color.transparent; color = Color.inherit; fontSize = 12.px
        }
        rule(".chip-x:hover") { backgroundColor = tc("error"); color = Color.white }
    }
}

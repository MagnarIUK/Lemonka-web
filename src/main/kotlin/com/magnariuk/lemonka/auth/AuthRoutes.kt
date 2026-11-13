package com.magnariuk.lemonka.auth

import com.magnariuk.lemonka.AppInfo
import com.magnariuk.lemonka.auth.AuthResult.Field
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.*
import com.magnariuk.lemonka.web.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

/** Login / registration page and the /auth endpoints. */
object AuthFeature : Feature {
    override fun Route.publicRoutes() {
        get("/login") {
            val session = call.sessions.get<UserSession>()
            if (session != null && AuthService.userFor(session.token) != null) {
                return@get call.hxRedirect(safeNext(call.parameters["next"]))
            }
            val state = call.loginState()
            call.respondPage("Вхід") { LoginScreen(state) }
        }
        // Switches between the login and register forms without reloading the page.
        get("/login/form") {
            val state = call.loginState()
            call.respondFragment { LoginForm(state) }
        }

        route("/auth") {
            post("/login") { submit(AuthMode.Login) { f -> AuthService.login(f["username"].orEmpty(), f["password"].orEmpty()) } }
            post("/register") {
                submit(AuthMode.Register) { f ->
                    AuthService.register(f["username"].orEmpty(), f["password"].orEmpty(), f["invite"])
                }
            }
            post("/logout") {
                call.sessions.get<UserSession>()?.let { AuthService.logout(it.token) }
                call.sessions.clear<UserSession>()
                call.hxRedirect("/login")
            }
        }
    }

    private suspend fun ApplicationCall.loginState() = LoginState(
        mode = if (parameters["mode"] == "register") AuthMode.Register else AuthMode.Login,
        next = parameters["next"],
        invite = parameters["invite"].orEmpty(),
        inviteRequired = AuthService.inviteRequired(),
    )

    private suspend fun RoutingContext.submit(mode: AuthMode, action: suspend (Parameters) -> AuthResult) {
        val form = call.receiveParameters()
        when (val r = action(form)) {
            is AuthResult.Success -> {
                call.sessions.set(UserSession(r.token.token))
                call.hxRedirect(safeNext(form["next"]))
            }
            is AuthResult.Failure -> {
                val state = LoginState(
                    mode = mode, username = form["username"].orEmpty(), invite = form["invite"].orEmpty(),
                    next = form["next"], error = r, inviteRequired = AuthService.inviteRequired(),
                )
                // HTMX swaps just the form; a plain form post (no JS) gets the whole page back.
                if (call.isHtmx) call.respondFragment { LoginForm(state) }
                else call.respondPage("Вхід") { LoginScreen(state) }
            }
        }
    }
}

enum class AuthMode { Login, Register }

data class LoginState(
    val mode: AuthMode = AuthMode.Login,
    val username: String = "",
    val invite: String = "",
    val next: String? = null,
    val error: AuthResult.Failure? = null,
    val inviteRequired: Boolean = true,
) {
    fun errorFor(field: Field) = error?.takeIf { it.field == field }?.message
}

fun FlowContent.LoginScreen(state: LoginState) = div("login-screen") {
    Card(classes = "login-card") {
        Column(gap = 6, align = Alignment.Center) {
            span("login-logo") { Icon(Icon.Subtitles) }
            h1 { +"Вітаємо!" }
            p("muted") { +"Увійдіть або створіть акаунт, щоб користуватися ${AppInfo.NAME}" }
        }
        LoginForm(state)
    }
}

fun FlowContent.LoginForm(state: LoginState) {
    val register = state.mode == AuthMode.Register
    val action = if (register) "/auth/register" else "/auth/login"
    form(action = action, method = FormMethod.post, classes = "login-form") {
        hxPost(action); hxTarget("this"); hxSwap("outerHTML")
        Column(gap = 14) {
            if (register && !state.inviteRequired) {
                div("login-note") { Icon(Icon.Shield); span { +"Це перший акаунт - він отримає права адміністратора." } }
            }
            TextField("Ім'я користувача", "username", state.username, required = true, autofocus = true,
                error = state.errorFor(Field.Username)) {
                attributes["autocomplete"] = "username"
                if (register) minLength = "${AuthService.MIN_USERNAME}"
            }
            TextField("Пароль", "password", type = InputType.password, required = true,
                error = state.errorFor(Field.Password)) {
                attributes["autocomplete"] = if (register) "new-password" else "current-password"
                if (register) minLength = "${AuthService.MIN_PASSWORD}"
            }
            if (register && state.inviteRequired) {
                TextField("Код запрошення", "invite", state.invite, placeholder = "XXXX-XXXX", required = true,
                    error = state.errorFor(Field.Invite)) { attributes["autocomplete"] = "off" }
            }
            state.next?.let { input(InputType.hidden, name = "next") { value = it } }

            Column(gap = 8, classes = "login-actions") {
                Button(if (register) "Зареєструватися" else "Увійти", ButtonVariant.Primary, type = ButtonType.submit)
                val other = if (register) AuthMode.Login else AuthMode.Register
                val query = buildString {
                    append("mode=").append(other.name.lowercase())
                    state.next?.let { append("&next=").append(it.encodeURLParameter()) }
                    if (state.invite.isNotEmpty()) append("&invite=").append(state.invite.encodeURLParameter())
                }
                Button(if (register) "Вже є акаунт? Увійти" else "Немає акаунта? Зареєструватися", ButtonVariant.Tertiary) {
                    hxGet("/login/form?$query"); hxTarget("closest form"); hxSwap("outerHTML")
                }
            }
        }
    }
}

object LoginStyle : Styled {
    override fun CssBuilder.styles() {
        rule(".login-screen") { display = Display.flex; justifyContent = JustifyContent.center; paddingTop = 6.vh }
        rule(".login-card") { width = 100.pct; maxWidth = 400.px; padding = Padding(32.px, 28.px) }
        rule(".login-card h1") { fontSize = 1.5.rem }
        rule(".login-card p") { textAlign = TextAlign.center; margin = Margin(0.px, 0.px, 20.px) }
        rule(".login-logo") {
            display = Display.flex; alignItems = Align.center; justifyContent = JustifyContent.center
            width = 56.px; height = 56.px; borderRadius = 16.px; marginBottom = 6.px
            backgroundColor = tc("primary"); color = Color.white; fontSize = 28.px
            put("box-shadow", "0 8px 20px ${v("primary-30")}")
        }
        rule(".login-actions") { marginTop = 6.px }
        rule(".login-actions .btn") { width = 100.pct }
        rule(".login-form") { put("animation", "fade-in .2s ${v("ease")}") }
        rule(".login-note") {
            display = Display.flex; gap = 8.px; alignItems = Align.center
            padding = Padding(10.px, 12.px); borderRadius = LinearDimension(v("radius-s"))
            backgroundColor = tc("primary-10"); color = tc("primary-text"); fontSize = 0.875.rem
        }
    }
}

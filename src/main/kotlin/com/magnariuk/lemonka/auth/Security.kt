package com.magnariuk.lemonka.auth

import com.magnariuk.lemonka.data.db.Role
import com.magnariuk.lemonka.data.db.TokenRepository
import com.magnariuk.lemonka.data.db.User
import com.magnariuk.lemonka.ui.components.EmptyState
import com.magnariuk.lemonka.ui.components.Icon
import com.magnariuk.lemonka.web.respondPage
import com.magnariuk.lemonka.web.hxRedirect
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

/**
 * The session is just the DB token, stored raw in the `token` cookie - the same cookie name and format
 * the old Spring version used, so people who were already logged in stay logged in.
 */
data class UserSession(val token: String)

const val AUTH = "auth-session"

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("token") {
            serializer = object : SessionSerializer<UserSession> {
                override fun serialize(session: UserSession) = session.token
                override fun deserialize(text: String) = UserSession(text)
            }
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = TokenRepository.lifetime.inWholeSeconds
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    install(Authentication) {
        session<UserSession>(AUTH) {
            validate { session -> AuthService.userFor(session.token) }
            challenge {
                call.sessions.clear<UserSession>()
                val next = call.request.uri.encodeURLParameter()
                call.hxRedirect("/login?next=$next")
            }
        }
    }
}

/** The logged-in user inside an `authenticate(AUTH) { }` block. */
val ApplicationCall.user: User get() = principal<User>()!!

/** Lets only users with [role] through; everyone else gets a 403 page. Used for admin routes. */
val RequireRole = createRouteScopedPlugin("RequireRole", ::RoleConfig) {
    val role = pluginConfig.role
    on(AuthenticationChecked) { call ->
        val user = call.principal<User>()
        if (user == null || (role == Role.Admin && !user.isAdmin)) {
            call.respondPage("Немає доступу", HttpStatusCode.Forbidden) {
                EmptyState("У вас немає доступу до цієї сторінки", Icon.Shield)
            }
        }
    }
}

class RoleConfig { var role: Role = Role.Admin }

fun Route.requireRole(role: Role, build: Route.() -> Unit): Route {
    val child = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
        override fun toString() = "(requireRole $role)"
    })
    child.install(RequireRole) { this.role = role }
    child.build()
    return child
}

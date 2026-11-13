package com.magnariuk.lemonka.web

import com.magnariuk.lemonka.AppInfo
import com.magnariuk.lemonka.auth.AUTH
import com.magnariuk.lemonka.auth.AuthFeature
import com.magnariuk.lemonka.auth.requireRole
import com.magnariuk.lemonka.data.db.Role
import com.magnariuk.lemonka.features.admin.AdminFeature
import com.magnariuk.lemonka.features.calculator.CalculatorFeature
import com.magnariuk.lemonka.features.home.HomeFeature
import com.magnariuk.lemonka.features.profile.ProfileFeature
import com.magnariuk.lemonka.features.sheet.SheetFeature
import com.magnariuk.lemonka.features.subtitles.SubtitlesFeature
import com.magnariuk.lemonka.ui.components.NOOP_URL
import com.magnariuk.lemonka.ui.theme.Stylesheet
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import com.magnariuk.lemonka.ui.components.EmptyState
import com.magnariuk.lemonka.ui.components.ToastTone
import io.ktor.server.response.*
import io.ktor.server.routing.*

/** Static assets and tiny UI helper endpoints. */
object UiFeature : Feature {
    override fun Route.publicRoutes() {
        get("/static/app.css") {
            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
            call.respondText(Stylesheet.css, ContentType.Text.CSS.withCharset(Charsets.UTF_8))
        }
        staticResources("/static", "static")
        get(NOOP_URL) { call.respondText("") }
    }
}

object Features {
    /** Order here = order in the navigation bar. */
    val all: List<Feature> = listOf(UiFeature, AuthFeature, HomeFeature, SubtitlesFeature, SheetFeature, CalculatorFeature, ProfileFeature, AdminFeature)
}

fun Application.installFeatures() {
    install(StatusPages) {
        exception<NotFoundException> { call, _ ->
            if (call.isHtmx) call.respondToast("Не знайдено", ToastTone.Error)
            else call.respondPage("Не знайдено", HttpStatusCode.NotFound) { EmptyState("Сторінку не знайдено") }
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondPage("Не знайдено", HttpStatusCode.NotFound) { EmptyState("Сторінку не знайдено") }
        }
    }
    installRoutes()
}

private fun Application.installRoutes() = routing {
    Features.all.forEach { with(it) { publicRoutes() } }
    authenticate(AUTH) {
        Features.all.forEach { with(it) { routes() } }
        requireRole(Role.Admin) {
            Features.all.forEach { with(it) { adminRoutes() } }
        }
    }
}

package com.magnariuk.lemonka.web

import com.magnariuk.lemonka.ui.layout.MenuItem
import com.magnariuk.lemonka.ui.layout.NavItem
import io.ktor.server.routing.*

/**
 * A pluggable part of the app (a page or tool). Each feature brings its own routes and, optionally,
 * an entry in the top navigation or the profile menu. Register features in [Features.all].
 */
interface Feature {
    val nav: NavItem? get() = null
    val menu: MenuItem? get() = null

    /** Public routes (no login needed). */
    fun Route.publicRoutes() {}

    /** Routes that require a logged-in user. */
    fun Route.routes() {}

    /** Routes only admins can reach. */
    fun Route.adminRoutes() {}
}

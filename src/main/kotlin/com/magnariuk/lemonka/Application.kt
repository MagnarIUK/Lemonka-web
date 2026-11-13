package com.magnariuk.lemonka

import com.magnariuk.lemonka.auth.configureSecurity
import com.magnariuk.lemonka.data.db.DatabaseFactory
import com.magnariuk.lemonka.web.installFeatures
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

/** Entry module, wired in application.yaml. Pages and tools are plugged in via Features. */
fun Application.module() {
    DatabaseFactory.init(environment.config.property("lemonka.dbPath").getString())
    configureSecurity()
    installFeatures()
}

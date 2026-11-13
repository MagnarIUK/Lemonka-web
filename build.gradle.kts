plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

group = "com.magnariuk"
version = "0.2.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "com.magnariuk.lemonka.ApplicationKt"
}

ktor {
    fatJar {
        archiveFileName = "lemonka-tools.jar"
    }
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.html)
    implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
    implementation(libs.kotlin.css)

    implementation(libs.bundles.exposed)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bcrypt)

    implementation(libs.logback)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

/** Everything the server needs, in one folder: build/docker/{Dockerfile, lemonka-tools.jar}. */
tasks.register<Sync>("dockerBundle") {
    group = "distribution"
    description = "Builds the fat jar and puts it next to the Dockerfile in build/docker"
    from(tasks.named("buildFatJar").map { it.outputs.files })
    from("docker/Dockerfile")
    into(layout.buildDirectory.dir("docker"))
}

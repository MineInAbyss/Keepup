plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadow)
    application
}

dependencies {
    implementation(project(":keepup-api"))

    implementation(libs.hocon)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kaml)
    implementation(libs.clikt)
    implementation(libs.turtle)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.mordant)
    implementation(libs.slf4j)
    testImplementation(kotlin("test"))
    implementation("io.pebbletemplates:pebble:3.2.2")
}

application {
    applicationName = "keepup"
    mainClass.set("com.mineinabyss.keepup.cli.MainKt")
}

distributions {
    this.shadow {
        distributionBaseName = "keepup"
    }
}

tasks {
    shadowJar {
        minimize {
            exclude { it.moduleGroup == "org.slf4j" || it.moduleGroup == "com.github.ajalt.mordant" }
        }
    }
}

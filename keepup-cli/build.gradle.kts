plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
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
    mainClass.set("com.mineinabyss.keepup.cli.MainKt")
}

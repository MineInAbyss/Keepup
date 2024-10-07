plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(idofrontLibs.plugins.mia.publication)
}

dependencies {
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.hocon)
    implementation(libs.kaml)
    implementation(libs.turtle)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.mordant)
    implementation("io.pebbletemplates:pebble:3.2.2")
}

java {
    withSourcesJar()
    withJavadocJar()
}

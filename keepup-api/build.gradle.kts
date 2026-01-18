plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.serialization)
    alias(idofrontLibs.plugins.mia.publication)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    implementation("com.mineinabyss.jsonschema-kt:annotations:0.1.1")
    implementation("com.mineinabyss.jsonschema-kt:dsl:0.1.1")
    implementation("com.mineinabyss.jsonschema-kt:generator-kotlinx-serialization:0.1.1")

    implementation(kotlin("test"))
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.hocon)
    implementation(libs.kaml)
    implementation(libs.turtle)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.mordant)
    implementation("io.pebbletemplates:pebble:3.2.3")
}

java {
    withSourcesJar()
    withJavadocJar()
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.mineinabyss"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

dependencies {
    implementation("com.typesafe:config:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.lordcodes.turtle:turtle:0.8.0")
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("com.github.ajalt.mordant:mordant:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("com.sealwu:kscript-tools:1.0.22")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.8")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    shadowJar {
        minimize()
    }
}

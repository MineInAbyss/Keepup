import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "com.mineinabyss"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
//    implementation("eu.jrie.jetbrains:kotlin-shell-core:0.2.1")
    implementation("com.lordcodes.turtle:turtle:0.8.0")
    testImplementation(kotlin("test"))
    implementation("com.jayway.jsonpath:json-path:2.7.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

application {
    mainClass.set("MainKt")
}

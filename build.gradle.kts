import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm) apply false version "2.3.0"
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
    }
}

//allprojects {
//    apply(plugin = "kotlin")
//
//    // Don't want to set a toolchain because we will be compiling with GraalVM 23 during CI
//    kotlin {
//        jvmToolchain(21)
//    }
//}

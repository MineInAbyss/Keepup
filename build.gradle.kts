import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

allprojects {
    repositories {
        mavenCentral()
    }
}

allprojects {
    apply(plugin = "kotlin")

    // Don't want to set a toolchain because we will be compiling with GraalVM 23 during CI
    kotlin {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

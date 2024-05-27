plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.41.0"
    id("nl.littlerobots.version-catalog-update") version "0.8.4"
    application
}

group = "com.mineinabyss"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-receivers")
    }
}

dependencies {
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

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.mineinabyss.keepup.commands.MainKt")
}

tasks {
    shadowJar {
        minimize {
            exclude { it.moduleGroup == "org.slf4j" }
        }
    }
}
versionCatalogUpdate {
    this.keep {
        keepUnusedPlugins = true
        keepUnusedVersions = true
        keepUnusedLibraries = true
    }
}

tasks.dependencyUpdates {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
}

allprojects {
    repositories {
        mavenCentral()
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

subprojects {
    apply(plugin = "kotlin")
    kotlin {
        jvmToolchain(22)
    }
}

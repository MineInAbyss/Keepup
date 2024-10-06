plugins {
    alias(libs.plugins.kotlinJvm)
//    id("com.github.johnrengelman.shadow") version "8.1.1"
//    id("com.github.ben-manes.versions") version "0.41.0"
//    id("nl.littlerobots.version-catalog-update") version "0.8.4"
}

allprojects {
    repositories {
        mavenCentral()
    }
}
//
//versionCatalogUpdate {
//    this.keep {
//        keepUnusedPlugins = true
//        keepUnusedVersions = true
//        keepUnusedLibraries = true
//    }
//}
//
//tasks.dependencyUpdates {
//    fun isNonStable(version: String): Boolean {
//        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
//        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
//        val isStable = stableKeyword || regex.matches(version)
//        return isStable.not()
//    }
//    rejectVersionIf {
//        isNonStable(candidate.version)
//    }
//}

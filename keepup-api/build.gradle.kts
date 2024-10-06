plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinx.serialization)
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
//    implementation(libs.clikt)
//    implementation(libs.slf4j)
    implementation("io.pebbletemplates:pebble:3.2.2")

}

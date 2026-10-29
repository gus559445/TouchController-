plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

dependencies {
    compileOnly(libs.fabric.loader)
    compileOnly(project(":mod:common"))
}

tasks.processResources {
    enabled = false
}

kotlin {
    jvmToolchain(8)
}

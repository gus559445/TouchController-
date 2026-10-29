plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

dependencies {
    compileOnly(libs.lwjgl)
    compileOnly(libs.lwjgl.glfw)
    compileOnly(project(":mod:common"))
}

kotlin {
    jvmToolchain(8)
}

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    id("TouchController.toolchain-conventions")
    id("top.fifthlight.stubgen")
}

val modVersion: String by extra.properties

group = "top.fifthlight.touchcontroller"
version = modVersion

minecraftStub {
    versions("1.20.1", "1.20.4", "1.20.6")
}

dependencies {
    compileOnly(project(":mod:common"))
    compileOnly(project(":mod:common-lwjgl3"))
    compileOnly(project(":combine"))
    compileOnly(libs.joml)
}

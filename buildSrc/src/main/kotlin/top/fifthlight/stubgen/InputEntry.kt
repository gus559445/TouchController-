package top.fifthlight.stubgen

import java.nio.file.Path

data class JarInput(
    val path: Path,
    val mapping: Path? = null,
)

data class InputEntry(val jars: List<JarInput>)

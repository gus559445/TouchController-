package top.fifthlight.touchcontroller.resource

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.StringWriter
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writer

@OptIn(ExperimentalSerializationApi::class)
fun generateLegacyText(languageDir: Path, legacyLanguageDir: Path) {
    val languageFiles = languageDir.listDirectoryEntries("*.json")
    for (file in languageFiles) {
        val outputFile = legacyLanguageDir.resolve("${file.nameWithoutExtension}.lang")
        val map: Map<String, String> = Json.decodeFromStream(file.inputStream())
        val writeBuffer = StringWriter()
        Properties().apply {
            map.entries.forEach { (key, value) ->
                put(key, value.replace("%d", "%s"))
            }
        }.store(writeBuffer, "PARSE_ESCAPES")
        outputFile.writer().use { writer ->
            writeBuffer
                .toString()
                .lineSequence()
                .filterIndexed { index, _ -> index != 1 }
                .forEach {
                    writer.write(it)
                    writer.write('\n'.code)
                }
        }
    }
}

package top.fifthlight.touchcontroller.resource

import com.squareup.kotlinpoet.*
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

fun generateBackgroundTexture(textureDir: Path, outputDir: Path) {
    val backgroundTexture = TypeSpec.objectBuilder("BackgroundTextures").run {
        val backgroundTextureTypeName = ClassName("top.fifthlight.combine.data", "BackgroundTexture")
        for (file in textureDir.listDirectoryEntries()) {
            val image = ImageIO.read(file.toFile())
            addProperty(
                PropertySpec
                    .builder(file.nameWithoutExtension.uppercase(), backgroundTextureTypeName)
                    .initializer(
                        """
                            BackgroundTexture(
                                identifier = Identifier.of("touchcontroller", "textures/gui/background/%L"),
                                size = IntSize(%L, %L),
                            )
                        """.trimIndent(),
                        file.name,
                        image.width,
                        image.height,
                    )
                    .build()
            )
        }
        build()
    }
    FileSpec
        .builder("top.fifthlight.touchcontroller.assets", "BackgroundTextures")
        .addAnnotation(
            AnnotationSpec
                .builder(Suppress::class)
                .addMember("%S", "RedundantVisibilityModifier")
                .build()
        )
        .addType(backgroundTexture)
        .addImport("top.fifthlight.data", "IntSize")
        .addImport("top.fifthlight.combine.data", "Identifier")
        .build()
        .writeTo(outputDir)
}
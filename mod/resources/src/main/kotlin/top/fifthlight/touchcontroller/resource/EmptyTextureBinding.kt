package top.fifthlight.touchcontroller.resource

import com.squareup.kotlinpoet.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path

fun generateEmptyTextureBinding(textures: Map<String, PlacedTexture>, outputDir: Path) {
    val textureTypeName = ClassName("top.fifthlight.combine.data", "NinePatchTexture")
    val identifierTypeName = ClassName("top.fifthlight.combine.data", "Identifier")
    val emptyTextureType = TypeSpec.enumBuilder("EmptyTexture").run {
        addAnnotation(Serializable::class)
        primaryConstructor(
            FunSpec
                .constructorBuilder()
                .addParameter(
                    ParameterSpec
                        .builder("texture", textureTypeName)
                        .build()
                )
                .addParameter(
                    ParameterSpec
                        .builder("nameId", identifierTypeName)
                        .build()
                )
                .build()
        )
        addProperty(
            PropertySpec
                .builder("texture", textureTypeName)
                .initializer("texture")
                .build()
        )
        addProperty(
            PropertySpec
                .builder("nameId", identifierTypeName)
                .initializer("nameId")
                .build()
        )
        for ((key, value) in textures.entries.filter { it.key.startsWith("EMPTY_") }.sortedBy { it.key }) {
            val fileName = value.relativePath.fileName.toString()
            if (!fileName.endsWith(".9.png", ignoreCase = true)) {
                continue
            }
            val name = fileName.uppercase().removeSuffix(".9.PNG")
            addEnumConstant(
                name,
                TypeSpec
                    .anonymousClassBuilder()
                    .addAnnotation(
                        AnnotationSpec
                            .builder(SerialName::class)
                            .addMember("%S", name.lowercase())
                            .build()
                    )
                    .addSuperclassConstructorParameter("Textures.%L", key)
                    .addSuperclassConstructorParameter("Texts.EMPTY_TEXTURE_%L", name)
                    .build()
            )
        }
        build()
    }
    val file = FileSpec
        .builder("top.fifthlight.touchcontroller.assets", "EmptyTexture")
        .addAnnotation(
            AnnotationSpec
                .builder(Suppress::class)
                .addMember("%S", "RedundantVisibilityModifier")
                .build()
        )
        .addImport("top.fifthlight.data", "IntSize", "IntOffset", "IntRect", "IntPadding")
        .addType(emptyTextureType)
        .build()
    file.writeTo(outputDir)
}
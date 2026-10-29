package top.fifthlight.stubgen

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import kotlin.io.path.outputStream

object InputRemapper {
    private class GetNameVisitor(visitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, visitor) {
        var name: String? = null

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String?>?
        ) {
            this.name = name
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }

    fun remap(input: JarInput, output: Path) {
        val mapping = input.mapping?.let(::readProguardMapping)
        val remapper = mapping?.let(::ProguardRemapper)
        val inputFile = JarFile(input.path.toFile())

        JarOutputStream(output.outputStream()).use { outputStream ->
            for (entry in inputFile.entries()) {

                if (!entry.name.endsWith(".class")) {
                    outputStream.putNextEntry(entry)
                    inputFile.getInputStream(entry).use { it.transferTo(outputStream) }
                    continue
                }

                val writer = ClassWriter(Opcodes.ASM9)
                val nameGetter = GetNameVisitor(writer)
                val classRemapper = ClassRemapper(nameGetter, remapper)
                ClassReader(inputFile.getInputStream(entry)).accept(classRemapper, 0)
                outputStream.putNextEntry(nameGetter.name?.let { name -> ZipEntry("$name.class") } ?: entry)
                outputStream.write(writer.toByteArray())
            }
        }
    }
}
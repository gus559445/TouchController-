package top.fifthlight.stubgen

import org.objectweb.asm.commons.Remapper
import java.nio.file.Path
import kotlin.io.path.bufferedReader

data class ProguardMapping(
    val classes: Map<String, ClassMapping>,
) {
    data class ClassMapping(
        val name: String,
        val fields: Map<String, String>,
        val methods: Map<ClassMap.MethodIdentifier, String>,
    )
}

class ProguardRemapper(
    private val mapping: ProguardMapping,
) : Remapper() {
    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        val clazz = mapping.classes[owner] ?: return name
        return clazz.fields[name] ?: name
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val clazz = mapping.classes[owner] ?: return name
        val identifier = ClassMap.MethodIdentifier(name, descriptor)
        return clazz.methods[identifier] ?: return name
    }

    override fun map(internalName: String): String = mapping.classes[internalName]?.name ?: internalName
}

private sealed class ReadState {
    data class MethodDescriptor(
        val arguments: List<String>,
        val returnType: String,
    )

    data class MethodIdentifier(
        val name: String,
        val descriptor: MethodDescriptor,
    )

    data class MutableClassMapping(
        val name: String,
        val obfName: String,
        val fields: MutableMap<String, String> = mutableMapOf(),
        val methods: MutableMap<MethodIdentifier, String> = mutableMapOf(),
    )

    data class MutableMapping(
        // Mapping of obfName -> name
        val classes: MutableMap<String, MutableClassMapping> = mutableMapOf(),
        // Mapping of name -> obfName
        val obfClasses: MutableMap<String, MutableClassMapping> = mutableMapOf(),
    ) {
        fun toMapping() = ProguardMapping(classes = classes.mapValues { (_, clazz) ->
            ProguardMapping.ClassMapping(
                name = clazz.name,
                fields = clazz.fields,
                methods = clazz.methods.mapKeys { (method, _) ->
                    val descriptor = buildString {
                        fun mapType(type: String) {
                            var typeName = type
                            while (typeName.endsWith("[]")) {
                                append('[')
                                typeName = typeName.substring(0, typeName.length - 2)
                            }
                            when (typeName) {
                                "byte" -> append('B')
                                "char" -> append('C')
                                "double" -> append('D')
                                "float" -> append('F')
                                "int" -> append('I')
                                "long" -> append('J')
                                "short" -> append('S')
                                "boolean" -> append('Z')
                                "void" -> append('V')
                                else -> {
                                    append('L')
                                    obfClasses[typeName]?.obfName?.let(::append) ?: append(typeName)
                                    append(';')
                                }
                            }
                        }

                        append('(')
                        method.descriptor.arguments.forEach(::mapType)
                        append(')')
                        mapType(method.descriptor.returnType)
                    }
                    ClassMap.MethodIdentifier(
                        name = method.name,
                        descriptor = descriptor,
                    )
                },
            )
        })
    }

    data class Context(
        val mapping: MutableMapping = MutableMapping(),
        var currentClass: MutableClassMapping? = null,
    )

    abstract fun acceptChar(ch: Char, context: Context): ReadState

    data class Spaces(private val nextState: (Char) -> ReadState) : ReadState() {
        override fun acceptChar(ch: Char, context: Context): ReadState = when (ch) {
            ' ' -> this
            '\n' -> error("Unexpected new line")
            else -> nextState(ch)
        }
    }

    object Start : ReadState() {
        override fun acceptChar(ch: Char, context: Context): ReadState = when {
            ch == '\n' -> this
            ch == ' ' -> this
            ch == '#' -> Comment
            ch.isDigit() -> LineNumber
            else -> Line.FirstToken(ch)
        }
    }

    object Comment : ReadState() {
        override fun acceptChar(ch: Char, context: Context): ReadState = if (ch == '\n') {
            Start
        } else {
            this
        }
    }

    object LineNumber : ReadState() {
        override fun acceptChar(ch: Char, context: Context): ReadState = if (ch == ':') {
            LineOrNumber
        } else {
            this
        }
    }

    object LineOrNumber : ReadState() {
        override fun acceptChar(ch: Char, context: Context): ReadState = if (ch.isDigit()) {
            LineNumber
        } else {
            Line.FirstToken(ch)
        }
    }

    sealed class Line : ReadState() {
        abstract class Token() : Line() {
            val buffer = StringBuilder()

            constructor(ch: Char) : this() {
                if (ch != ' ') {
                    buffer.append(ch)
                }
            }

            final override fun acceptChar(ch: Char, context: Context): ReadState = when (ch) {
                '.' -> acceptTokenChar('/', context)
                else -> acceptTokenChar(ch, context)
            }

            abstract fun acceptTokenChar(ch: Char, context: Context): ReadState
        }

        data class Arrow(private val nextState: (Char) -> ReadState) : Line() {
            override fun acceptChar(ch: Char, context: Context): ReadState = when (ch) {
                '>' -> Spaces(nextState)
                else -> error("Bad mapping: Invalid character before '-': $ch")
            }
        }

        class FirstToken(ch: Char) : Token(ch) {
            override fun acceptTokenChar(ch: Char, context: Context): ReadState = when (ch) {
                ' ' -> Spaces { char ->
                    when (char) {
                        '-' -> Arrow { Class(buffer.toString(), it) }
                        else -> MethodOrField(buffer.toString(), char)
                    }
                }

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }

        class Class(private val name: String, ch: Char) : Token(ch) {
            override fun acceptTokenChar(ch: Char, context: Context) = when (ch) {
                ':' -> {
                    val obfName = buffer.toString()
                    val classMapping = MutableClassMapping(
                        name = name,
                        obfName = obfName,
                    )
                    context.mapping.classes[obfName] = classMapping
                    context.mapping.obfClasses[name] = classMapping
                    context.currentClass = classMapping
                    ClassEnd
                }

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }

        object ClassEnd : Line() {
            override fun acceptChar(ch: Char, context: Context): ReadState = when (ch) {
                '\n' -> Start
                else -> error("Unknown character after class: $ch")
            }
        }

        class MethodOrField(private val firstToken: String, ch: Char) : Token(ch) {
            override fun acceptTokenChar(ch: Char, context: Context): ReadState = when (ch) {
                ' ' -> Spaces { char ->
                    when (char) {
                        '-' -> Arrow { arrowChar ->
                            FieldObfName(
                                type = firstToken,
                                name = buffer.toString(),
                                ch = arrowChar,
                            )
                        }

                        else -> error("Field have no obf name")
                    }
                }

                '(' -> MethodArgument(
                    returnType = firstToken,
                    name = buffer.toString(),
                    arguments = mutableListOf(),
                )

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }

        class FieldObfName(
            private val type: String,
            private val name: String,
            ch: Char
        ) : Token(ch) {
            override fun acceptTokenChar(ch: Char, context: Context): ReadState = when (ch) {
                '\n' -> {
                    val obfName = buffer.toString()
                    val currentClass = context.currentClass ?: error("No current class for field $name")
                    currentClass.fields[obfName] = name
                    Start
                }

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }

        data class MethodArgument(
            private val returnType: String,
            private val name: String,
            private var arguments: MutableList<String>,
        ) : Token() {
            override fun acceptTokenChar(ch: Char, context: Context): ReadState = when (ch) {
                ',' -> {
                    arguments.add(buffer.toString())
                    buffer.clear()
                    this
                }

                ')' -> {
                    if (buffer.isNotEmpty()) {
                        arguments.add(buffer.toString())
                        buffer.clear()
                    }
                    Spaces { char ->
                        when (char) {
                            '-' -> Arrow {
                                MethodObfName(
                                    returnType = returnType,
                                    name = name,
                                    arguments = arguments,
                                    ch = it
                                )
                            }

                            else -> error("No obf name for method $name")
                        }
                    }
                }

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }

        class MethodObfName(
            private val returnType: String,
            private val name: String,
            private val arguments: List<String>,
            ch: Char,
        ) : Token(ch) {
            override fun acceptTokenChar(ch: Char, context: Context): ReadState = when (ch) {
                '\n' -> {
                    val obfName = buffer.toString()
                    val currentClass = context.currentClass ?: error("No current class for field $name")
                    val methodIdentifier = MethodIdentifier(
                        name = obfName,
                        descriptor = MethodDescriptor(
                            arguments = arguments,
                            returnType = returnType,
                        ),
                    )
                    currentClass.methods[methodIdentifier] = name
                    Start
                }

                else -> {
                    buffer.append(ch)
                    this
                }
            }
        }
    }
}

fun readProguardMapping(path: Path) = path.bufferedReader().use {
    var ch = it.read()
    var state: ReadState = ReadState.Start
    var context = ReadState.Context()
    while (ch != -1) {
        state = state.acceptChar(ch.toChar(), context)
        ch = it.read()
    }
    context.mapping.toMapping()
}

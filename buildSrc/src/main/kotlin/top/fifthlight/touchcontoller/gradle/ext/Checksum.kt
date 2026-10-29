package top.fifthlight.touchcontoller.gradle.ext

import java.security.MessageDigest

fun String.sha1sum(): ByteArray = MessageDigest.getInstance("SHA-1").digest(toByteArray())
fun ByteArray.toHexString(): String = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }

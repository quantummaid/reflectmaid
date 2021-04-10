package de.quantummaid.reflectmaid.bytecodeexecutor.compilation

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

class ClassJavaFileObject(className: String,
                          kind: JavaFileObject.Kind) : SimpleJavaFileObject(memUri(className, kind), kind) {
    private val outputStream: ByteArrayOutputStream = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return outputStream
    }

    fun bytes(): ByteArray {
        return outputStream.toByteArray()
    }
}

class StringJavaFileObject(name: String,
                           private val code: String) : SimpleJavaFileObject(stringUri(name), JavaFileObject.Kind.SOURCE) {

    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
        return code
    }
}

fun memUri(name: String, kind: JavaFileObject.Kind): URI {
    return javaFileUri("mem", name, kind)
}

private fun stringUri(name: String): URI {
    return javaFileUri("string", name, JavaFileObject.Kind.SOURCE)
}

private fun javaFileUri(type: String, name: String, kind: JavaFileObject.Kind): URI {
    val path = name.replace('.', '/')
    val extension = kind.extension
    return URI.create("$type:///$path$extension")
}

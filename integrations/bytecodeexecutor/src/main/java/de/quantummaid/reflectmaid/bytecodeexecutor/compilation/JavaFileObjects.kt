/**
 * Copyright (c) 2021 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

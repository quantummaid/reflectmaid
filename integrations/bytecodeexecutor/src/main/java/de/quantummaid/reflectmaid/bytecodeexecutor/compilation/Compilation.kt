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

import de.quantummaid.reflectmaid.bytecodeexecutor.compilation.SimpleJavaFileManager.Companion.fromCompiler
import java.security.AccessController.doPrivileged
import java.security.PrivilegedAction
import java.util.*
import javax.tools.*

class InMemoryCompiler(private val compiler: JavaCompiler,
                       private val fileManager: JavaFileManager) {

    companion object {
        fun createInMemoryCompiler(): InMemoryCompiler {
            val compiler = ToolProvider.getSystemJavaCompiler()
            val fileManager = fromCompiler(compiler)
            return InMemoryCompiler(compiler, fileManager)
        }
    }

    fun compileAndLoad(program: String, className: String): Class<*> {
        compile(program, className)
        val classLoader = (fileManager as SimpleJavaFileManager).createClassLoader()
        return classLoader.loadClass(className)
    }

    private fun compile(program: String, className: String) {
        val compilationUnit = StringJavaFileObject(className, program)
        val compilationTask = compiler.getTask(
                null,
                fileManager,
                null,
                null,
                null,
                listOf(compilationUnit)
        )
        val success = compilationTask.call()
        if (!success) {
            throw IllegalStateException("compilation of class $className was not successful\n\n\n$program")
        }
    }
}


private class SimpleJavaFileManager(fileManager: JavaFileManager) : ForwardingJavaFileManager<JavaFileManager>(fileManager) {
    private val outputFiles = ArrayList<ClassJavaFileObject>()

    override fun getJavaFileForOutput(location: JavaFileManager.Location,
                                      className: String,
                                      kind: JavaFileObject.Kind,
                                      sibling: FileObject): JavaFileObject {
        val file = ClassJavaFileObject(className, kind)
        outputFiles.add(file)
        return file
    }

    fun createClassLoader(): CompiledClassLoader {
        return doPrivileged(PrivilegedAction { CompiledClassLoader(outputFiles) })
    }

    companion object {
        fun fromCompiler(compiler: JavaCompiler): SimpleJavaFileManager {
            val standardFileManager = compiler.getStandardFileManager(null, null, null)
            return SimpleJavaFileManager(standardFileManager)
        }
    }
}

private class CompiledClassLoader(private val files: MutableList<ClassJavaFileObject>) : ClassLoader() {
    override fun findClass(name: String): Class<*> {
        val uri = memUri(name, JavaFileObject.Kind.CLASS)
        return files
                .filter { it.toUri() == uri }
                .map {
                    val bytes = it.bytes()
                    super.defineClass(name, bytes, 0, bytes.size)
                }
                .firstOrNull()
                ?: super.findClass(name)
    }
}


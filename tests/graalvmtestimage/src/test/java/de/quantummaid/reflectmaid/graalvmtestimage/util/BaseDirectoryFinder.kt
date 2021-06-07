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
package de.quantummaid.reflectmaid.graalvmtestimage.util

import java.io.File
import java.lang.RuntimeException
import java.util.*

val PROJECT_BASE_PATH = BaseDirectoryFinder.findProjectBaseDirectory()

object BaseDirectoryFinder {
    private const val PROJECT_ROOT_ANCHOR_FILENAME = ".projectrootanchor"
    private val PROJECT_ROOT_DIRECTORY = computeProjectBaseDirectory()

    fun findProjectBaseDirectory(): String {
        return PROJECT_ROOT_DIRECTORY
    }

    private fun computeProjectBaseDirectory(): String {
        val codeSourceUrl = BaseDirectoryFinder::class.java.protectionDomain.codeSource.location
        val codeSourceLocation = codeSourceUrl.file
        val currentDirectory = System.getProperty("user.dir")
        return computeProjectBaseDirectoryFrom(codeSourceLocation)
                .or { computeProjectBaseDirectoryFrom(currentDirectory) }
                .orElseThrow {
                    BaseDirectoryNotFoundException(String.format("unable to find project root directory (code source URL: %s, current working directory: %s)",
                            codeSourceUrl, currentDirectory))
                }
    }

    private fun computeProjectBaseDirectoryFrom(startDirectory: String): Optional<String> {
        var currentDirectory = File(startDirectory)
        while (!anchorFileIn(currentDirectory).exists()) {
            if (isRootDirectory(currentDirectory)) {
                return Optional.empty()
            }
            currentDirectory = parentOf(currentDirectory)
        }
        return Optional.of(currentDirectory.absolutePath)
    }

    private fun anchorFileIn(parent: File): File {
        return File(parent, PROJECT_ROOT_ANCHOR_FILENAME)
    }

    private fun isRootDirectory(f: File): Boolean {
        return f.parent == null
    }

    private fun parentOf(directory: File): File {
        return File(directory.parent)
    }
}

class BaseDirectoryNotFoundException(message: String?) : RuntimeException(message)
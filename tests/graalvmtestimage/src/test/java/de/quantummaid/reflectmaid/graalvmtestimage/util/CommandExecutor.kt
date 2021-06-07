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

import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*

data class CommandExecution(val exitCode: Int,
                            val stdout: String,
                            val stderr: String,
                            val duration: Duration)

fun runCommand(env: Map<String, String>, command: List<String?>?): CommandExecution {
    val envArray = env.entries
            .map { entry: Map.Entry<String, String> -> entry.key + "=" + entry.value }
            .toTypedArray()
    val commandLine = java.lang.String.join(" ", command)
    println(commandLine)
    return try {
        val begin = Instant.now()
        val process = Runtime.getRuntime().exec(commandLine, envArray, null)
        val exitCode = process.waitFor()
        val end = Instant.now()
        val duration = Duration.between(begin, end)
        val output = inputStreamToString(process.inputStream)
        val error = inputStreamToString(process.errorStream)
        val commandExecution = CommandExecution(exitCode, output, error, duration)
        if (exitCode != 0) {
            throw RuntimeException(commandExecution.toString())
        }
        commandExecution
    } catch (e: IOException) {
        throw RuntimeException(e)
    } catch (e: InterruptedException) {
        throw RuntimeException(e)
    }
}

fun inputStreamToString(inputStream: InputStream?): String {
    val scanner = Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A")
    return if (scanner.hasNext()) {
        scanner.next()
    } else ""
}
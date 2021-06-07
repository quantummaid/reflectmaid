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
package de.quantummaid.reflectmaid.graalvmtestimage

import de.quantummaid.reflectmaid.graalvmtestimage.util.BaseDirectoryFinder.findProjectBaseDirectory
import de.quantummaid.reflectmaid.graalvmtestimage.util.runCommand
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class GraalVmTestImageSmoke {
    private val executable = findProjectBaseDirectory() + "/tests/graalvmtestimage/target/executable"

    @Test
    fun graalVmTestImageSmokeTest() {
        val (exitCode, stdout, stderr, _) = runCommand(emptyMap(), listOf(executable))
        assertThat(exitCode, `is`(0))
        assertThat(stderr, `is`(""))
        assertThat(
            stdout, `is`(
                "" +
                        "proxy works\n" +
                        "fields: 3\n" +
                        "methods: 10\n" +
                        "constructors: 1\n"
            )
        )
    }
}
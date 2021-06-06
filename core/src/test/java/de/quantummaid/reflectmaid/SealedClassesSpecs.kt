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
package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

sealed class SealedClass

class SubClass0 : SealedClass()
class SubClass1 : SealedClass()
sealed class SealedSubClass : SealedClass()
class SubSubClass0 : SealedSubClass()
class SubSubClass1 : SealedSubClass()

class SealedClassesSpecs {

    @Test
    fun sealedSubclassesCanBeFound() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<SealedClass>()
        val sealedSubclasses = resolvedType.sealedSubclasses()
        assertThat(sealedSubclasses.map { it.simpleDescription() }, containsInAnyOrder("SubClass0", "SubClass1", "SealedSubClass"))
    }

    @Test
    fun nestedSealedSubclassesCanBeFound() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<SealedClass>()
        val sealedSubclasses = resolvedType.sealedSubclasses()
        val nestedSealedSubclasses = sealedSubclasses.flatMap { it.sealedSubclasses() }
        assertThat(nestedSealedSubclasses.map { it.simpleDescription() }, containsInAnyOrder("SubSubClass0", "SubSubClass1"))
    }

    @Test
    fun sealedSubclassesForJavaClassesAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<InputStream>()
        assertThat(resolvedType.sealedSubclasses(), empty())
    }

    @Test
    fun sealedSubclassesForArraysAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<Array<SealedClass>>()
        assertTrue(resolvedType.isArray)
        assertThat(resolvedType.sealedSubclasses(), empty())
    }

    @Test
    fun sealedSubclassesForWildcardsAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        assertThat(resolvedType.sealedSubclasses(), empty())
    }
}
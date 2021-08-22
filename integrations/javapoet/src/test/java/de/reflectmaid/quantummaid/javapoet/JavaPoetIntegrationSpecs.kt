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
package de.reflectmaid.quantummaid.javapoet

import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import de.quantummaid.reflectmaid.ReflectMaid
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class JavaPoetIntegrationSpecs {

    @Test
    fun classNameOfNormalClassCanBeMapped() {
        assertMapping<String>("java.lang.String")
    }

    @Test
    fun classNameOfClassWithTypeParametersCanBeMapped() {
        assertMapping<List<String>>("java.util.List<java.lang.String>")
    }

    @Test
    fun classNameOfBoxedPrimitiveCanBeMapped() {
        assertMapping<Int>("java.lang.Integer")
    }

    @Test
    fun classNameOfPrimitiveCanBeMapped() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<List<Any>>()
        val sizeMethod = resolvedType.methods().first { it.name == "size" }
        val intType = sizeMethod.returnType!!
        val typeName = intType.toTypeName()
        assertThat(typeName.toString(), `is`("int"))
    }

    @Test
    fun classNameOfArrayCanBeMapped() {
        assertMapping<Array<String>>("java.lang.String[]")
    }

    @Test
    fun classNameOfWildcardCanBeMapped() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        val typeName = resolvedType.toTypeName()
        assertThat(typeName.toString(), `is`("java.lang.Object"))
    }

    private inline fun <reified T: Any> assertMapping(asString: String) {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<T>()
        val typeName = resolvedType.toTypeName()
        assertThat(typeName.toString(), `is`(asString))
    }
}
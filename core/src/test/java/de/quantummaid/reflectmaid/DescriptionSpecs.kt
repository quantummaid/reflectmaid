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

import de.quantummaid.reflectmaid.types.KotlinType
import de.quantummaid.reflectmaid.types.TestType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test

class DescriptionSpecs {

    @Test
    fun kotlinMethodsCanBeDescribed() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<KotlinType>()
        val methods = resolvedType.methods().map { it.describe() }
        assertThat(methods, containsInAnyOrder(
                "'fun voidMethod(parameter0: String)' [public final void de.quantummaid.reflectmaid.types.KotlinType.voidMethod(java.lang.String)]",
                "'fun nonVoidMethod(parameter0: String, parameter1: Array<Integer>): String' [public final java.lang.String de.quantummaid.reflectmaid.types.KotlinType.nonVoidMethod(java.lang.String,java.lang.Integer[])]"
        ))
    }

    @Test
    fun javaMethodsCanBeDescribed() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TestType>()
        val methods = resolvedType.methods().map { it.describe() }
        assertThat(methods, containsInAnyOrder(
                "'String method()' [public java.lang.String de.quantummaid.reflectmaid.types.TestType.method()]"
        ))
    }
}
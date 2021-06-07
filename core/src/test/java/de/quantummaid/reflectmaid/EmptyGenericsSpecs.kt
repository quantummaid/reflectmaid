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

import de.quantummaid.reflectmaid.ReflectMaid.Companion.aReflectMaid
import de.quantummaid.reflectmaid.types.TypeWithEmptyGenerics
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class EmptyGenericsSpecs {

    @Test
    fun reflectMaidCanDealWithEmptyGenerics() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithEmptyGenerics>()

        val fields = resolvedType.fields()
        assertThat(fields.size, `is`(1))
        assertThat(fields[0].type.simpleDescription(), `is`("List<Object>"))

        val constructors = resolvedType.constructors()
        assertThat(constructors.size, `is`(1))
        assertThat(constructors[0].parameters[0].type.simpleDescription(), `is`("List<Object>"))

        val methods = resolvedType.methods()
        assertThat(methods.size, `is`(1))
        assertThat(methods[0].returnType!!.simpleDescription(), `is`("List<Object>"))
    }
}
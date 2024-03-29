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

import de.quantummaid.reflectmaid.types.TestTypeWithConstructorThatThrows
import de.quantummaid.reflectmaid.types.TestTypeWithMethodThatThrows
import de.quantummaid.reflectmaid.types.TypeWithPublicFields
import de.quantummaid.reflectmaid.util.withException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test

class ExecutorSpecs {

    @Test
    fun methodCanBeExecutedByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<String>()
        val method = resolvedType.methods()
                .filter { it.name == "strip" }
                .first { it.parameters.isEmpty() }
        val executor = method.createExecutor()
        val result = executor.execute("    abc    ", listOf())
        assertThat(result, `is`("abc"))
    }

    @Test
    fun staticMethodCanBeExecutedByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val method = resolvedType.methods().first { it.name == "concat" }
        val executor = method.createExecutor()
        val result = executor.execute(null, listOf("a", "b"))
        assertThat(result, `is`("ab"))
    }

    @Test
    fun constructorCanBeExecutedByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<ArrayList<String>>()
        val constructor = resolvedType.constructors()
                .filter { it.parameters.size == 1 }
                .first {
                    val description = it.parameters[0].type.description()
                    description == "java.util.Collection<java.lang.String>"
                }
        val executor = constructor.createExecutor()
        val result = executor.execute(null, listOf(listOf("a", "b", "c")))
        assertThat(result, instanceOf(ArrayList::class.java))
        assertThat(result as List<*>, contains("a", "b", "c"))
    }

    @Test
    fun fieldCanBeGottenByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "field" }
        val getter = field.createGetter()
        val publicField = TypeWithPublicFields()
        publicField.field = "foo"
        val result = getter.get(publicField)
        assertThat(result, `is`("foo"))
    }

    @Test
    fun staticFieldCanBeGottenByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val setter = field.createGetter()
        TypeWithPublicFields.staticField = "foo"
        val result = setter.get(null)
        assertThat(result, `is`("foo"))
    }

    @Test
    fun fieldCanBeSetByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "field" }
        val setter = field.createSetter()
        val publicField = TypeWithPublicFields()
        publicField.field = "foo"
        setter.set(publicField, "bar")
        assertThat(publicField.field, `is`("bar"))
    }

    @Test
    fun staticFieldCanBeSetByReflection() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val setter = field.createSetter()
        TypeWithPublicFields.staticField = "foo"
        setter.set(null, "bar")
        assertThat(TypeWithPublicFields.staticField, `is`("bar"))
    }

    @Test
    fun exceptionInConstructorIsDirectlyThrown() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TestTypeWithConstructorThatThrows>()
        val constructor = resolvedType.constructors()[0]
        val executor = constructor.createExecutor()
        val exception = withException<UnsupportedOperationException> { executor.execute(null, listOf()) }
        assertThat(exception.message, `is`("foo"))
        assertThat(exception.stackTrace[0].className, `is`("de.quantummaid.reflectmaid.types.TestTypeWithConstructorThatThrows"))
    }

    @Test
    fun exceptionInMethodIsDirectlyThrown() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<TestTypeWithMethodThatThrows>()
        val method = resolvedType.methods()[0]
        val executor = method.createExecutor()
        val instance = TestTypeWithMethodThatThrows()
        val exception = withException<UnsupportedOperationException> { executor.execute(instance, listOf()) }
        assertThat(exception.message, `is`("foo"))
        assertThat(exception.stackTrace[0].className, `is`("de.quantummaid.reflectmaid.types.TestTypeWithMethodThatThrows"))
    }
}
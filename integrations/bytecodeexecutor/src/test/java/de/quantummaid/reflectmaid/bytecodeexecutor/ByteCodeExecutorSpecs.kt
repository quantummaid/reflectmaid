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
package de.quantummaid.reflectmaid.bytecodeexecutor

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.bytecodeexecutor.ByteCodeExecutorFactory.Companion.byteCodeExecutorFactory
import de.quantummaid.reflectmaid.bytecodeexecutor.types.TypeWithPublicFields
import de.quantummaid.reflectmaid.bytecodeexecutor.types.TypeWithPublicFieldsAndTypeVariable
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test

class ByteCodeExecutorSpecs {

    @Test
    fun methodCanBeExecuted() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<String>()
        val method = resolvedType.methods()
                .filter { it.name == "strip" }
                .first { it.parameters.isEmpty() }
        val executor = method.createExecutor()
        val result = executor.execute("    abc    ", listOf())
        assertThat(result, `is`("abc"))
    }

    @Test
    fun methodOfClassWithTypeVariableCanBeExecuted() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<List<String>>()
        val method = resolvedType.methods()
                .filter { it.name == "get" }
                .first { it.parameters.size == 1 }
        val executor = method.createExecutor()
        val list = ArrayList<String>()
        list.add("foooo")
        val result = executor.execute(list, listOf(0))
        assertThat(result, `is`("foooo"))
    }

    @Test
    fun staticMethodCanBeExecuted() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val method = resolvedType.methods().first { it.name == "concat" }
        val executor = method.createExecutor()
        val result = executor.execute(null, listOf("a", "b"))
        assertThat(result, `is`("ab"))
    }

    @Test
    fun methodWithTypeVariableParameterCanBeExecuted() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<List<String>>()
        val method = resolvedType.methods()
                .filter { it.name == "add" }
                .first { it.parameters.size == 1 }
        val executor = method.createExecutor()
        val list = ArrayList<String>()
        executor.execute(list, listOf("asdf"))
        assertThat(list[0], `is`("asdf"))
    }

    @Test
    fun constructorCanBeExecuted() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
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
    fun fieldCanBeGotten() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
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
    fun staticFieldCanBeGotten() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val getter = field.createGetter()
        TypeWithPublicFields.staticField = "foo"
        val result = getter.get(null)
        assertThat(result, `is`("foo"))
    }

    @Test
    fun staticFieldOfClassWithTypeVariableCanBeGotten() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFieldsAndTypeVariable<Int>>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val getter = field.createGetter()
        TypeWithPublicFieldsAndTypeVariable.staticField = "foo"
        val result = getter.get(null)
        assertThat(result, `is`("foo"))
    }

    @Test
    fun fieldCanBeSet() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
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
    fun fieldOfClassWithTypeVariableCanBeSet() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFieldsAndTypeVariable<Int>>()
        val field = resolvedType.fields()
                .first { it.name == "field" }
        val setter = field.createSetter()
        val publicField = TypeWithPublicFieldsAndTypeVariable<Int>()
        publicField.field = 1
        setter.set(publicField, 2)
        assertThat(publicField.field, `is`(2))
    }

    @Test
    fun staticFieldCanBeSet() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFields>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val setter = field.createSetter()
        TypeWithPublicFields.staticField = "foo"
        setter.set(null, "bar")
        assertThat(TypeWithPublicFields.staticField, `is`("bar"))
    }

    @Test
    fun staticFieldOfClassWithTypeVariableCanBeSet() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val resolvedType = reflectMaid.resolve<TypeWithPublicFieldsAndTypeVariable<Int>>()
        val field = resolvedType.fields()
                .first { it.name == "staticField" }
        val setter = field.createSetter()
        TypeWithPublicFieldsAndTypeVariable.staticField = "foo"
        setter.set(null, "bar")
        assertThat(TypeWithPublicFieldsAndTypeVariable.staticField, `is`("bar"))
    }
}
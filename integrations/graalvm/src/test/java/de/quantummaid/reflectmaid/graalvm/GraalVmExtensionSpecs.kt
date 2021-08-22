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
package de.quantummaid.reflectmaid.graalvm

import de.quantummaid.reflectmaid.ReflectMaid.Companion.aReflectMaid
import de.quantummaid.reflectmaid.createDynamicProxyFactory
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import java.io.InputStream

fun interface MyInterface {
    fun foo()
}

class GraalVmExtensionSpecs {

    @Test
    fun reflectionsCanBeRegisteredToGraalVm() {
        val reflectMaidRegistry = ReflectMaidRegistry()

        val reflectMaid = aReflectMaid()
        reflectMaid.registerToGraalVm(registry = reflectMaidRegistry)

        reflectMaid.resolve<MyInterface>()

        var reflections: List<ClassType>? = null
        reflectMaidRegistry.registerReflections { reflections = it }
        assertThat(reflections, `is`(notNullValue()))
        assertThat(reflections!!.map { it.simpleDescription() }, contains("MyInterface"))
        assertThat(reflections!![0].cachedMethods().size, `is`(0))
        assertThat(reflections!![0].cachedConstructors().size, `is`(0))
        assertThat(reflections!![0].cachedFields().size, `is`(0))

        var dynamicProxies: List<Class<*>>? = null
        reflectMaidRegistry.registerDynamicProxies { dynamicProxies = it }
        assertThat(dynamicProxies, `is`(nullValue()))
    }

    @Test
    fun dynamicProxyCanBeRegisteredToGraalVm() {
        val reflectMaidRegistry = ReflectMaidRegistry()

        val reflectMaid = aReflectMaid()
        reflectMaid.registerToGraalVm(registry = reflectMaidRegistry)

        reflectMaid.createDynamicProxyFactory<MyInterface>()

        var dynamicProxies: List<Class<*>>? = null
        reflectMaidRegistry.registerDynamicProxies { dynamicProxies = it }
        assertThat(dynamicProxies, `is`(notNullValue()))
        assertThat(dynamicProxies, contains(MyInterface::class.java))

        var reflections: List<ClassType>? = null
        reflectMaidRegistry.registerReflections { reflections = it }
        assertThat(reflections, `is`(notNullValue()))
        assertThat(
            reflections!!.map { it.simpleDescription() }, containsInAnyOrder(
                "MyInterface", "Object", "long", "int", "boolean", "String", "Class<Object>"
            )
        )
        assertThat(reflections!![0].cachedMethods().size, `is`(1))
        assertThat(reflections!![0].cachedConstructors().size, `is`(0))
        assertThat(reflections!![0].cachedFields().size, `is`(0))
    }

    @Test
    fun multipleReflectMaidsCanBeRegisteredToGraalVm() {
        val reflectMaidRegistry = ReflectMaidRegistry()

        val reflectMaid0 = aReflectMaid()
        reflectMaid0.registerToGraalVm(registry = reflectMaidRegistry)
        reflectMaid0.resolve<String>()

        val reflectMaid1 = aReflectMaid()
        reflectMaid1.registerToGraalVm(registry = reflectMaidRegistry)
        reflectMaid1.resolve<InputStream>()

        val reflectMaid2 = aReflectMaid()
        reflectMaid2.registerToGraalVm(registry = reflectMaidRegistry)
        reflectMaid2.resolve<List<Int>>()

        var reflections: List<ClassType>? = null
        reflectMaidRegistry.registerReflections { reflections = it }
        assertThat(reflections, `is`(notNullValue()))
        assertThat(
            reflections!!.map { it.simpleDescription() }, containsInAnyOrder(
                "String",
                "InputStream",
                "List<Integer>",
                "Integer"
            )
        )

        var dynamicProxies: List<Class<*>>? = null
        reflectMaidRegistry.registerDynamicProxies { dynamicProxies = it }
        assertThat(dynamicProxies, `is`(nullValue()))
    }

    @Test
    fun reflectionRegistrationCanBeDisabled() {
        val reflectMaidRegistry = ReflectMaidRegistry()

        val reflectMaid = aReflectMaid()
        reflectMaid.registerToGraalVm(registerReflections = false, registry = reflectMaidRegistry)

        reflectMaid.resolve<MyInterface>()

        var reflections: List<ClassType>? = null
        reflectMaidRegistry.registerReflections { reflections = it }
        assertThat(reflections, `is`(nullValue()))

        var dynamicProxies: List<Class<*>>? = null
        reflectMaidRegistry.registerDynamicProxies { dynamicProxies = it }
        assertThat(dynamicProxies, `is`(nullValue()))
    }

    @Test
    fun dynamicProxyRegistrationCanBeDisabled() {
        val reflectMaidRegistry = ReflectMaidRegistry()

        val reflectMaid = aReflectMaid()
        reflectMaid.registerToGraalVm(registerDynamicProxies = false, registry = reflectMaidRegistry)

        reflectMaid.createDynamicProxyFactory<MyInterface>()

        var dynamicProxies: List<Class<*>>? = null
        reflectMaidRegistry.registerDynamicProxies { dynamicProxies = it }
        assertThat(dynamicProxies, `is`(nullValue()))

        var reflections: List<ClassType>? = null
        reflectMaidRegistry.registerReflections { reflections = it }
        assertThat(reflections, `is`(notNullValue()))
        assertThat(
            reflections!!.map { it.simpleDescription() }, containsInAnyOrder(
                "MyInterface", "Object", "long", "int", "boolean", "String", "Class<Object>"
            )
        )
        assertThat(reflections!![0].cachedMethods().size, `is`(1))
        assertThat(reflections!![0].cachedConstructors().size, `is`(0))
        assertThat(reflections!![0].cachedFields().size, `is`(0))
    }
}
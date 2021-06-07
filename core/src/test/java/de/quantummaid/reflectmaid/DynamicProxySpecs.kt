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
import de.quantummaid.reflectmaid.util.withException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

interface MyInterface {
    fun call(parameter: String): String
}

interface MyTypedInterface<T> {
    fun call(parameter0: T, parameter1: T): T
}

interface MyMultiMethodInterface {
    fun method0(): String

    fun method1(): String
}

class DynamicProxySpecs {

    @Test
    fun dynamicProxyCanBeCreated() {
        val reflectMaid = aReflectMaid()
        val proxyFactory = reflectMaid.createDynamicProxyFactory<MyInterface>()
        val proxy = proxyFactory.createProxy { _, parameters ->
            "found: " + parameters[0]
        }

        val result = proxy.call("foooo")
        assertThat(result, `is`("found: foooo"))
    }

    @Test
    fun dynamicProxyInterfaceCanHaveTypeVariables() {
        val reflectMaid = aReflectMaid()

        val stringProxyFactory = reflectMaid.createDynamicProxyFactory<MyTypedInterface<String>>()
        val stringProxy = stringProxyFactory.createProxy { _, parameters ->
            parameters[0] as String + parameters[1] as String
        }
        val stringResult = stringProxy.call("foo", "bar")
        assertThat(stringResult, `is`("foobar"))

        val proxyFactory = reflectMaid.createDynamicProxyFactory<MyTypedInterface<Int>>()
        val intProxy = proxyFactory.createProxy { _, parameters ->
            parameters[0] as Int + parameters[1] as Int
        }
        val intResult = intProxy.call(1, 2)
        assertThat(intResult, `is`(3))
    }

    @Test
    fun dynamicProxyInterfaceCanHaveMultipleMethods() {
        val reflectMaid = aReflectMaid()
        val proxyFactory = reflectMaid.createDynamicProxyFactory<MyMultiMethodInterface>()
        val proxy = proxyFactory.createProxy { method, _ ->
            when (method.name()) {
                "method0" -> "foo"
                "method1" -> "bar"
                else -> throw UnsupportedOperationException()
            }
        }

        assertThat(proxy.method0(), `is`("foo"))
        assertThat(proxy.method1(), `is`("bar"))
    }

    @Test
    fun dynamicProxyCannotBeCreatedOnClass() {
        val reflectMaid = aReflectMaid()
        val exception = withException<DynamicProxyException> {
            reflectMaid.createDynamicProxyFactory<String>()
        }
        assertThat(
            exception.message, `is`(
                "type 'java.lang.String' needs to be an interface" +
                        " to be used as a dynamic proxy facade"
            )
        )
    }
}
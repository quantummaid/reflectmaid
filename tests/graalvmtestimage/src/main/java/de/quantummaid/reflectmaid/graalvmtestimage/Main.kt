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

import de.quantummaid.reflectmaid.ProxyFactory
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.createDynamicProxyFactory
import de.quantummaid.reflectmaid.graalvm.registerToGraalVm

data class MyDto(val field0: String, val field1: String, val field2: String)

fun interface MyInterface {
    fun doSomething()
}

fun createProxyFactoryAndRegisterType(): ProxyFactory<MyInterface> {
    val reflectMaid = ReflectMaid.aReflectMaid()
    reflectMaid.registerToGraalVm()
    val resolvedType = reflectMaid.resolve<MyDto>()
    resolvedType.methods()
    resolvedType.constructors()
    resolvedType.fields()
    return reflectMaid.createDynamicProxyFactory()
}

val PROXY_FACTORY = createProxyFactoryAndRegisterType()

fun main() {
    val myInterface = PROXY_FACTORY.createProxy { _, _ -> println("proxy works") }
    myInterface.doSomething()

    val otherReflectMaid = ReflectMaid.aReflectMaid()
    val resolvedType = otherReflectMaid.resolve<MyDto>()
    println("fields: ${resolvedType.fields().size}")
    println("methods: ${resolvedType.methods().size}")
    println("constructors: ${resolvedType.constructors().size}")
}
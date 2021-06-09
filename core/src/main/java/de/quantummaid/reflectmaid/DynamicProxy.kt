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

import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

fun interface ProxyHandler {
    fun invoke(method: ResolvedMethod, parameters: List<Any?>): Any?
}

fun interface ProxyFactory<T> {
    fun createProxy(handler: ProxyHandler): T
}

inline fun <reified T : Any> ReflectMaid.createDynamicProxyFactory(): ProxyFactory<T> {
    val resolvedType = resolve<T>()
    return createDynamicProxyFactory(resolvedType)
}

fun <T : Any> ReflectMaid.createDynamicProxyFactory(facadeInterface: Class<T>): ProxyFactory<T> {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxyFactory(resolvedType)
}

fun <T : Any> ReflectMaid.createDynamicProxyFactory(facadeInterface: KClass<T>): ProxyFactory<T> {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxyFactory(resolvedType)
}

fun <T : Any> ReflectMaid.createDynamicProxyFactory(facadeInterface: GenericType<T>): ProxyFactory<T> {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxyFactory(resolvedType)
}

fun <T : Any> ReflectMaid.createDynamicProxyFactory(facadeInterface: ResolvedType): ProxyFactory<T> {
    if (!facadeInterface.isInterface()) {
        throw DynamicProxyException(
            "type '${facadeInterface.description()}' needs to be an interface to be used " +
                    "as a dynamic proxy facade"
        )
    }
    return executorFactory.createDynamicProxyFactory(facadeInterface)
}

fun <T> createDynamicProxyFactoryUsingInvocationHandler(facadeInterface: ResolvedType): ProxyFactory<T> {
    val methods = facadeInterface.methods().associateBy { it.method }
    val assignableType = facadeInterface.assignableType()
    val classLoader = assignableType.classLoader
    return InvocationHandlerProxyFactory(methods, classLoader, assignableType)
}

class InvocationHandlerProxyFactory<T>(
    private val methods: Map<Method, ResolvedMethod>,
    private val classLoader: ClassLoader,
    private val assignableType: Class<*>
) : ProxyFactory<T> {

    @Suppress("UNCHECKED_CAST")
    override fun createProxy(handler: ProxyHandler): T {
        val invocationHandler = InternalInvocationHandler(handler, methods)
        val proxyInstance = Proxy.newProxyInstance(
            classLoader,
            arrayOf(assignableType),
            invocationHandler
        )
        return proxyInstance as T
    }
}

internal class InternalInvocationHandler(
    private val handler: ProxyHandler,
    private val methods: Map<Method, ResolvedMethod>
) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val resolvedMethod = methods[method]!!
        val parameters = args?.toList() ?: emptyList<Any?>()
        return handler.invoke(resolvedMethod, parameters)
    }
}

class DynamicProxyException(message: String) : RuntimeException(message)
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
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import java.lang.reflect.InvocationTargetException

interface Executor {
    fun execute(instance: Any?, parameters: List<Any?>): Any?
}

interface Setter {
    fun set(instance: Any?, value: Any?)
}

interface Getter {
    fun get(instance: Any?): Any?
}

interface ExecutorFactory {
    fun createMethodExecutor(method: ResolvedMethod): Executor
    fun createConstructorExecutor(constructor: ResolvedConstructor): Executor
    fun createFieldGetter(field: ResolvedField): Getter
    fun createFieldSetter(field: ResolvedField): Setter
    fun <T> createDynamicProxyFactory(facadeInterface: ResolvedType, reflectMaid: ReflectMaid): ProxyFactory<T>
}

class ReflectionExecutorFactory : ExecutorFactory {
    private val registeredDynamicProxies = ArrayList<ResolvedType>()

    override fun createMethodExecutor(method: ResolvedMethod) = ReflectionMethodExecutor(method)
    override fun createConstructorExecutor(constructor: ResolvedConstructor) =
        ReflectionConstructorExecutor(constructor)

    override fun createFieldGetter(field: ResolvedField) = ReflectionFieldGetter(field)
    override fun createFieldSetter(field: ResolvedField) = ReflectionFieldSetter(field)
    override fun <T> createDynamicProxyFactory(
        facadeInterface: ResolvedType,
        reflectMaid: ReflectMaid
    ): ProxyFactory<T> {
        registeredDynamicProxies.add(facadeInterface)
        return createDynamicProxyFactoryUsingInvocationHandler(facadeInterface, reflectMaid)
    }

    fun registeredDynamicProxies(): List<ResolvedType> {
        return registeredDynamicProxies
    }
}

class ReflectionMethodExecutor(private val method: ResolvedMethod) : Executor {

    override fun execute(instance: Any?, parameters: List<Any?>): Any? {
        try {
            return method.method.invoke(instance, *parameters.toTypedArray())
        } catch (e: InvocationTargetException) {
            throw handleInvocationTargetException(
                e,
                "calling method ${method.describe()} in ${method.declaringType.description()}"
            )
        }
    }
}

class ReflectionConstructorExecutor(private val constructor: ResolvedConstructor) : Executor {

    override fun execute(instance: Any?, parameters: List<Any?>): Any? {
        try {
            return constructor.constructor.newInstance(*parameters.toTypedArray())
        } catch (e: InvocationTargetException) {
            throw handleInvocationTargetException(
                e,
                "calling constructor ${constructor.describe()} in ${constructor.declaringType.description()}"
            )
        }
    }
}

class ReflectionFieldGetter(private val field: ResolvedField) : Getter {
    override fun get(instance: Any?): Any? {
        return field.field.get(instance)
    }
}

class ReflectionFieldSetter(private val field: ResolvedField) : Setter {
    override fun set(instance: Any?, value: Any?) {
        field.field.set(instance, value)
    }
}

private fun handleInvocationTargetException(
    exception: InvocationTargetException,
    description: String
): java.lang.Exception {
    val targetException = exception.targetException
    if (targetException is java.lang.Exception) {
        return targetException
    } else {
        throw ReflectionExecutionException("unexpected error $description", exception)
    }
}

class ReflectionExecutionException(message: String, cause: Throwable) : RuntimeException(message, cause)
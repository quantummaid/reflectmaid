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
package de.quantummaid.reflectmaid.queries

import de.quantummaid.reflectmaid.Executor
import de.quantummaid.reflectmaid.Getter
import de.quantummaid.reflectmaid.Setter
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod

sealed class TypedGetter<T>(private val previous: TypedGetter<*>?) {
    @Suppress("UNCHECKED_CAST")
    fun get(instance: Any?): T? {
        val actualInstance = if (previous != null) {
            previous.get(instance)
        } else {
            instance
        }
        return rawGet(actualInstance) as T?
    }

    internal abstract fun rawGet(instance: Any?): Any?

    companion object {
        fun <T> getterFromField(field: ResolvedField, previous: TypedGetter<*>?): TypedGetter<T> {
            return if (field.isPublic()) {
                val raw = field.createGetter()
                TypedFieldGetter(raw, previous)
            } else {
                val accessorMethod = field.kotlinGetAccessor()
                if (accessorMethod != null) {
                    getterFromMethod(accessorMethod, previous)
                } else {
                    throw UnsupportedOperationException("unable to create getter for field '${field.describe()}'")
                }
            }
        }

        fun <T> getterFromMethod(method: ResolvedMethod, previous: TypedGetter<*>?): TypedGetter<T> {
            if (!canHaveFollowUp(method)) {
                throw UnsupportedOperationException("cannot create getter from method ${method.describe()}")
            }
            val raw = method.createExecutor()
            return TypedMethodGetter(raw, previous)
        }
    }
}

private class TypedFieldGetter<T>(private val raw: Getter, previous: TypedGetter<*>?) : TypedGetter<T>(previous) {

    override fun rawGet(instance: Any?): Any? {
        return raw.get(instance)
    }
}

private class TypedMethodGetter<T>(private val raw: Executor, previous: TypedGetter<*>?) : TypedGetter<T>(previous) {

    override fun rawGet(instance: Any?): Any? {
        return raw.execute(instance, emptyList())
    }
}

sealed class TypedSetter<T>(private val previous: TypedGetter<*>?) {
    fun set(instance: Any?, value: T?) {
        val actualInstance = if (previous != null) {
            previous.get(instance)
        } else {
            instance
        }
        rawSet(actualInstance, value)
    }

    internal abstract fun rawSet(instance: Any?, value: T?): Any?

    companion object {
        fun <T> setterFromField(field: ResolvedField, previous: TypedGetter<*>?): TypedSetter<T> {
            return if (field.isPublic() && !field.isFinal()) {
                val setter = field.createSetter()
                TypedFieldSetter(setter, previous)
            } else {
                val accessorMethod = field.kotlinSetAccessor()
                if (accessorMethod != null) {
                    setterFromMethod(accessorMethod, previous)
                } else {
                    throw UnsupportedOperationException("unable to create setter for field '${field.describe()}' because it is final")
                }
            }
        }

        fun <T> setterFromMethod(method: ResolvedMethod, previous: TypedGetter<*>?): TypedSetter<T> {
            if (method.parameters.size != 1 || method.returnType != null) {
                throw UnsupportedOperationException("unable to create setter from method ${method.describe()}")
            }
            val executor = method.createExecutor()
            return TypedMethodSetter(executor, previous)
        }
    }
}

private class TypedFieldSetter<T>(private val raw: Setter, previous: TypedGetter<*>?) : TypedSetter<T>(previous) {

    @Suppress("UNCHECKED_CAST")
    override fun rawSet(instance: Any?, value: T?) {
        raw.set(instance, value)
    }
}

private class TypedMethodSetter<T>(private val raw: Executor, previous: TypedGetter<*>?) : TypedSetter<T>(previous) {

    @Suppress("UNCHECKED_CAST")
    override fun rawSet(instance: Any?, value: T?) {
        raw.execute(instance, listOf(value))
    }
}
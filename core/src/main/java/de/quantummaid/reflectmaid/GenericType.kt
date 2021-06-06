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

import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

sealed class GenericType<T> {
    companion object {
        @JvmStatic
        fun <T> genericType(type: Class<T>): GenericType<T> {
            return genericType(type, emptyList())
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, typeVariables: List<GenericType<*>>): GenericType<T> {
            return GenericTypeFromClass(type, typeVariables)
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, vararg typeVariables: GenericType<*>): GenericType<T> {
            return genericType(type, typeVariables.toList())
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, vararg typeVariables: Class<*>): GenericType<T> {
            return GenericTypeFromClass(type, typeVariables.map { genericType(it) })
        }

        @JvmStatic
        fun <T> genericType(typeToken: TypeToken<T>): GenericType<T> {
            return GenericTypeFromToken(typeToken)
        }

        inline fun <reified T : Any> genericType(): GenericType<T> {
            return genericType(object : TypeToken<T>() {})
        }

        fun <T : Any> genericType(type: KClass<T>): GenericType<T> {
            return genericType(type, emptyList())
        }

        fun <T : Any> genericType(type: KClass<*>, typeVariables: List<GenericType<*>>): GenericType<T> {
            return GenericTypeFromKClass(type, typeVariables)
        }

        fun <T : Any> genericType(type: KClass<*>, vararg typeVariables: GenericType<*>): GenericType<T> {
            return genericType(type, typeVariables.toList())
        }

        fun <T : Any> genericType(type: KClass<*>, vararg typeVariables: KClass<*>): GenericType<T> {
            return genericType(type, typeVariables.map { genericType(it) })
        }

        @JvmStatic
        fun wildcard(): GenericType<*> {
            return GenericTypeWildcard()
        }

        @JvmStatic
        fun <T : Any> fromResolvedType(resolvedType: ResolvedType): GenericType<T> {
            return GenericTypeFromResolvedType(resolvedType)
        }

        @JvmStatic
        fun <T : Any> fromReflectionType(type: Type, genericContext: ClassType): GenericType<T> {
            return GenericTypeFromReflectionType(type, genericContext)
        }
    }
}

data class GenericTypeFromClass<T>(val type: Class<*>,
                                   val typeVariables: List<GenericType<*>>) : GenericType<T>()

data class GenericTypeFromKClass<T : Any>(val kClass: KClass<*>,
                                          val typeVariables: List<GenericType<*>>) : GenericType<T>()

data class GenericTypeFromToken<T>(val typeToken: TypeToken<T>) : GenericType<T>()

class GenericTypeWildcard : GenericType<Any>() {
    override fun equals(other: Any?): Boolean {
        return other is GenericTypeWildcard
    }

    override fun hashCode(): Int {
        return 1
    }
}

data class GenericTypeFromResolvedType<T>(val resolvedType: ResolvedType) : GenericType<T>()

data class GenericTypeFromReflectionType<T>(val type: Type, val genericContext: ClassType) : GenericType<T>()
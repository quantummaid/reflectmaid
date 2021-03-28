/*
 * Copyright (c) 2020 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.quantummaid.reflectmaid.resolvedtype.resolver

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.exceptions.UnresolvableTypeVariableException
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import java.util.stream.Collectors

data class ResolvedMethod(val returnType: ResolvedType?,
                          val parameters: List<ResolvedParameter>,
                          val method: Method) {

    fun returnType(): Optional<ResolvedType> {
        return Optional.ofNullable(returnType)
    }

    fun hasParameters(parameters: List<ResolvedType>): Boolean {
        if (parameters.size != this.parameters.size) {
            return false
        }
        for (i in parameters.indices) {
            if (parameters[i] != this.parameters[i].type) {
                return false
            }
        }
        return true
    }

    fun name(): String {
        return method.name
    }

    val isPublic: Boolean
        get() {
            val modifiers = method.modifiers
            return Modifier.isPublic(modifiers)
        }

    val isStatic: Boolean
        get() {
            val modifiers = method.modifiers
            return Modifier.isStatic(modifiers)
        }

    fun describe(): String {
        val parametersString = parameters.stream()
                .map { resolvedParameter: ResolvedParameter ->
                    val type = resolvedParameter.type.simpleDescription()
                    val parameterName = resolvedParameter.parameter.name
                    String.format("%s %s", type, parameterName)
                }
                .collect(Collectors.joining(", "))
        val returnTypeDescription = Optional.ofNullable(returnType)
                .map { type: ResolvedType -> type.assignableType().simpleName }
                .orElse("void")
        val name = method.name
        val fullSignature = method.toGenericString()
        return String.format(
                "'%s %s(%s)' [%s]",
                returnTypeDescription,
                name,
                parametersString,
                fullSignature)
    }

    companion object {
        @JvmStatic
        fun resolveMethodsWithResolvableTypeVariables(reflectMaid: ReflectMaid,
                                                      fullType: ClassType): List<ResolvedMethod> {
            val type = fullType.assignableType()
            return type.declaredMethods
                    .filter { !it.isSynthetic }
                    .mapNotNull {
                        try {
                            resolveMethod(reflectMaid, it, fullType)
                        } catch (e: UnresolvableTypeVariableException) {
                            null
                        }
                    }
        }

        fun resolveMethod(reflectMaid: ReflectMaid,
                          method: Method,
                          context: ClassType): ResolvedMethod {
            val genericReturnType = method.genericReturnType
            val parameters = ResolvedParameter.resolveParameters(reflectMaid, method, context)
            val returnType: ResolvedType? = if (genericReturnType !== Void.TYPE) {
                reflectMaid.resolve(fromReflectionType<Any>(genericReturnType, context))
            } else {
                null
            }
            return ResolvedMethod(returnType, parameters, method)
        }
    }
}
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
package de.quantummaid.reflectmaid.resolvedtype.resolver

import de.quantummaid.reflectmaid.Executor
import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.languages.Language
import de.quantummaid.reflectmaid.languages.ParameterData
import de.quantummaid.reflectmaid.resolvedtype.Cached
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.UnresolvableTypeVariableException
import de.quantummaid.reflectmaid.RawClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

data class ResolvedMethod(
    val returnType: ResolvedType?,
    val parameters: List<ResolvedParameter>,
    val declaringType: ResolvedType,
    val method: Method,
    val language: Language,
    val reflectMaid: ReflectMaid
) {
    private val executor: Cached<Executor> = Cached { reflectMaid.executorFactory.createMethodExecutor(this) }

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

    fun name(): String = method.name

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

    fun describe() = describe(language)

    fun describe(language: Language): String {
        val parameterData = parameters.map { ParameterData(it.parameter.name, it.type.simpleDescription(language)) }
        val returnTypeDescription = returnType?.simpleDescription(language)
        val methodDescription = language.method(method.name, parameterData, returnTypeDescription)
        return "'$methodDescription' [${method.toGenericString()}]"
    }

    fun createExecutor() = executor.get()

    companion object {
        fun resolveMethodsWithResolvableTypeVariables(
            reflectMaid: ReflectMaid,
            fullType: ClassType,
            raw: RawClass,
            language: Language
        ): List<ResolvedMethod> {
            return raw.declaredMethods()
                .filter { !it.isSynthetic }
                .mapNotNull {
                    try {
                        resolveMethod(reflectMaid, it, fullType, language)
                    } catch (e: UnresolvableTypeVariableException) {
                        null
                    }
                }
        }

        private fun resolveMethod(
            reflectMaid: ReflectMaid,
            method: Method,
            context: ClassType,
            language: Language
        ): ResolvedMethod {
            val genericReturnType = method.genericReturnType
            val parameters = ResolvedParameter.resolveParameters(reflectMaid, method, context)
            val returnType: ResolvedType? = if (genericReturnType !== Void.TYPE) {
                reflectMaid.resolve(fromReflectionType<Any>(genericReturnType, context))
            } else {
                null
            }
            return ResolvedMethod(returnType, parameters, context, method, language, reflectMaid)
        }
    }
}
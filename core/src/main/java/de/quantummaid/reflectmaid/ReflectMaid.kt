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

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.RawClassCache.Companion.rawTypeCache
import de.quantummaid.reflectmaid.TypeVariableName.Companion.typeVariableName
import de.quantummaid.reflectmaid.resolvedtype.ArrayType
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithGenerics
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithoutGenerics
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.WildcardedType
import kotlin.reflect.KClass

class ReflectMaid(
    private val cache: ReflectionCache,
    internal val rawClassCache: RawClassCache,
    val executorFactory: ExecutorFactory
) {

    fun resolve(type: Class<*>): ResolvedType {
        val genericType = genericType(type)
        return resolve(genericType)
    }

    fun resolve(type: KClass<*>): ResolvedType {
        val genericType = genericType(type)
        return resolve(genericType)
    }

    inline fun <reified T : Any> resolve(): ResolvedType {
        val genericType = genericType<T>()
        return resolve(genericType)
    }

    fun resolve(genericType: GenericType<*>): ResolvedType {
        return cache.lookUp(genericType) { resolveInternal(it) }
    }

    fun registeredTypes(): Collection<ResolvedType> {
        return cache.registeredResolvedTypes()
    }

    private fun resolveInternal(genericType: GenericType<*>): ResolvedType {
        return when (genericType) {
            is GenericTypeFromClass -> resolveClass(genericType)
            is GenericTypeFromToken -> {
                resolveFromTypeToken(genericType.typeToken)
            }
            is GenericTypeFromKClass -> {
                return resolve(GenericTypeFromClass<Any>(genericType.kClass.java, genericType.typeVariables))
            }
            is GenericTypeWildcard -> {
                return WildcardedType()
            }
            is GenericTypeFromResolvedType -> {
                return genericType.resolvedType
            }
            is GenericTypeFromReflectionType -> {
                val (type, context) = genericType
                resolveType(this, type, context)
            }
        }
    }

    private fun resolveFromTypeToken(typeToken: TypeToken<*>): ResolvedType {
        val subclass = rawClassCache.rawClassFor(typeToken.javaClass)
        val genericSupertype = subclass.genericSuperType()!!
        val subclassType = fromClassWithoutGenerics(this, subclass)
        val interfaceType = resolve(fromReflectionType<Any>(genericSupertype, subclassType)) as ClassType
        return interfaceType.typeParameter(typeVariableName("T"))
    }

    private fun resolveClass(genericType: GenericTypeFromClass<*>): ResolvedType {
        val (type, typeVariables) = genericType
        val rawType = rawClassCache.rawClassFor(type)
        if (rawType.isArray()) {
            val componentType = rawType.componentType()!!
            val resolvedComponentType = resolve(componentType)
            return ArrayType(resolvedComponentType, this)
        }
        val resolvedParameters = typeVariables.map { resolveInternal(it) }
        return resolve(rawType, resolvedParameters)
    }

    private fun resolve(type: RawClass, variableValues: List<ResolvedType>): ResolvedType {
        val variableNames = type.typeParameters()
            .map { typeVariableName(it) }
        validateVariablesSameSizeAsVariableNames(type, variableNames, variableValues)
        val resolvedParameters = HashMap<TypeVariableName, ResolvedType>(variableValues.size)
        for (i in variableNames.indices) {
            val name = variableNames[i]
            val value = variableValues[i]
            resolvedParameters[name] = value
        }
        return if (resolvedParameters.isEmpty()) {
            fromClassWithoutGenerics(this, type)
        } else {
            fromClassWithGenerics(this, type, resolvedParameters)
        }
    }

    private fun validateVariablesSameSizeAsVariableNames(
        type: RawClass,
        variableNames: List<TypeVariableName>,
        variableValues: List<ResolvedType>
    ) {
        if (variableValues.size != variableNames.size) {
            val variables = variableNames.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.name }
            throw GenericTypeException(
                "type '${type.name()}' contains the following type variables that need " +
                        "to be filled in in order to create a ${GenericType::class.java.simpleName} object: ${variables}"
            )
        }
    }

    companion object {
        @JvmStatic
        fun aReflectMaid(): ReflectMaid {
            val executorFactory = ReflectionExecutorFactory()
            return aReflectMaid(executorFactory)
        }

        @JvmStatic
        fun aReflectMaid(executorFactory: ExecutorFactory): ReflectMaid {
            return ReflectMaid(ReflectionCache(), rawTypeCache(), executorFactory)
        }
    }
}

class GenericTypeException(message: String) : RuntimeException(message)
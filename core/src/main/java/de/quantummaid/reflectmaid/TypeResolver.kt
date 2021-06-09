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
import de.quantummaid.reflectmaid.TypeVariableName.Companion.typeVariableName
import de.quantummaid.reflectmaid.resolvedtype.ArrayType
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithGenerics
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithoutGenerics
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.WildcardedType
import java.lang.reflect.*

internal fun resolveType(
    reflectMaid: ReflectMaid,
    type: Type,
    context: ClassType
): ResolvedType {
    return when (type) {
        is Class<*> -> resolveClass(reflectMaid, type)
        is TypeVariable<*> -> resolveTypeVariable(type, context)
        is ParameterizedType -> resolveParameterizedType(reflectMaid, type, context)
        is GenericArrayType -> resolveGenericArrayType(reflectMaid, type, context)
        is WildcardType -> resolveWildcard(reflectMaid, type, context)
        else -> throw UnsupportedJvmFeatureInTypeException(
            "Unknown 'Type' implementation by class '${type.javaClass}' on object '$type'"
        )
    }
}

internal fun resolveClass(
    reflectMaid: ReflectMaid,
    clazz: Class<*>,
): ResolvedType {
    val raw = reflectMaid.rawClassCache.rawClassFor(clazz)
    return when {
        raw.isArray() -> {
            val componentType = reflectMaid.resolve(raw.componentType()!!)
            ArrayType(componentType, reflectMaid)
        }
        raw.typeParameters().isNotEmpty() -> {
            val any = reflectMaid.resolve(Any::class.java)
            val emptyTypeParameters = clazz.typeParameters.associate { typeVariableName(it) to any }
            fromClassWithGenerics(reflectMaid, raw, emptyTypeParameters)
        }
        else -> {
            fromClassWithoutGenerics(reflectMaid, raw)
        }
    }
}

private fun resolveTypeVariable(
    typeVariable: TypeVariable<*>,
    fullType: ClassType
): ResolvedType {
    val typeVariableName = typeVariableName(typeVariable)
    return fullType.resolveTypeVariable(typeVariableName)
}

private fun resolveParameterizedType(
    reflectMaid: ReflectMaid,
    parameterizedType: ParameterizedType,
    context: ClassType
): ResolvedType {
    val rawType = reflectMaid.rawClassCache.rawClassFor(parameterizedType.rawType as Class<*>)
    val typeVariableNames = rawType.typeParameters().map { typeVariableName(it) }
    val actualTypeArguments = parameterizedType.actualTypeArguments
    val typeParameters: MutableMap<TypeVariableName, ResolvedType> = HashMap(actualTypeArguments.size)
    for (i in actualTypeArguments.indices) {
        val resolvedTypeArgument = reflectMaid.resolve(fromReflectionType<Any>(actualTypeArguments[i], context))
        val name = typeVariableNames[i]
        typeParameters[name] = resolvedTypeArgument
    }
    return fromClassWithGenerics(reflectMaid, rawType, typeParameters)
}

private fun resolveGenericArrayType(
    reflectMaid: ReflectMaid,
    genericArrayType: GenericArrayType,
    context: ClassType
): ArrayType {
    val componentType = genericArrayType.genericComponentType
    val fullComponentType = resolveType(reflectMaid, componentType, context)
    return ArrayType(fullComponentType, reflectMaid)
}

private fun resolveWildcard(
    reflectMaid: ReflectMaid,
    type: WildcardType,
    context: ClassType
): ResolvedType {
    return if (type.lowerBounds.isEmpty() && type.upperBounds.size == 1) {
        val upperBound = type.upperBounds[0]
        resolveType(reflectMaid, upperBound, context)
    } else {
        WildcardedType()
    }
}

class UnsupportedJvmFeatureInTypeException(message: String) : UnsupportedOperationException(message)
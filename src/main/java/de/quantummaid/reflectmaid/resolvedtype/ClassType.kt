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
package de.quantummaid.reflectmaid.resolvedtype

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.TypeVariableName
import de.quantummaid.reflectmaid.exceptions.UnresolvableTypeVariableException
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor.Companion.resolveConstructors
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField.Companion.resolvedFields
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod.Companion.resolveMethodsWithResolvableTypeVariables
import de.quantummaid.reflectmaid.validators.NotNullValidator
import java.lang.reflect.Modifier
import java.util.stream.Collectors

data class ClassType(private val clazz: Class<*>,
                     private val typeParameters: Map<TypeVariableName, ResolvedType>,
                     private val reflectMaid: ReflectMaid) : ResolvedType {
    private var methods: Cached<List<ResolvedMethod>> = Cached { resolveMethodsWithResolvableTypeVariables(reflectMaid, this) }
    private var constructors: Cached<List<ResolvedConstructor>> = Cached { resolveConstructors(reflectMaid, this) }
    private var fields: Cached<List<ResolvedField>> = Cached { resolvedFields(reflectMaid, this) }

    fun typeParameter(name: TypeVariableName): ResolvedType {
        require(typeParameters.containsKey(name)) { "No type parameter with the name: " + name.name() }
        return typeParameters[name]!!
    }

    fun resolveTypeVariable(name: TypeVariableName): ResolvedType {
        if (!typeParameters.containsKey(name)) {
            throw UnresolvableTypeVariableException.unresolvableTypeVariableException(name)
        }
        return typeParameters[name]!!
    }

    override fun typeParameters(): List<ResolvedType> {
        return TypeVariableName.typeVariableNamesOf(clazz)
                .map { typeParameters[it]!! }
    }

    fun methods(): List<ResolvedMethod> {
        return methods.get()
    }

    fun constructors(): List<ResolvedConstructor> {
        return constructors.get()
    }

    fun fields(): List<ResolvedField> {
        return fields.get()
    }

    override fun description(): String {
        if (typeParameters.isEmpty()) {
            return clazz.name
        }
        val parametersString = typeParameters().stream()
                .map { obj: ResolvedType -> obj.description() }
                .collect(Collectors.joining(", ", "<", ">"))
        return clazz.name + parametersString
    }

    override fun simpleDescription(): String {
        if (typeParameters.isEmpty()) {
            return clazz.simpleName
        }
        val parametersString = typeParameters().stream()
                .map { obj: ResolvedType -> obj.simpleDescription() }
                .collect(Collectors.joining(", ", "<", ">"))
        return clazz.simpleName + parametersString
    }

    override val isPublic: Boolean
        get() {
            val modifiers = clazz.modifiers
            return Modifier.isPublic(modifiers)
        }
    override val isAbstract: Boolean
        get() = if (clazz.isPrimitive) {
            false
        } else Modifier.isAbstract(clazz.modifiers)
    override val isInterface: Boolean
        get() = clazz.isInterface
    override val isAnonymousClass: Boolean
        get() = clazz.isAnonymousClass
    override val isInnerClass: Boolean
        get() = clazz.enclosingClass != null
    override val isLocalClass: Boolean
        get() = clazz.isLocalClass
    override val isStatic: Boolean
        get() {
            val modifiers = clazz.modifiers
            return Modifier.isStatic(modifiers)
        }
    override val isAnnotation: Boolean
        get() = clazz.isAnnotation
    override val isWildcard: Boolean
        get() = false

    override fun assignableType(): Class<*> {
        return clazz
    }

    companion object {
        @JvmStatic
        fun fromClassWithoutGenerics(reflectMaid: ReflectMaid,
                                     type: Class<*>): ClassType {
            NotNullValidator.validateNotNull(type, "type")
            if (type.isArray) {
                throw UnsupportedOperationException()
            }
            if (type.typeParameters.isNotEmpty()) {
                throw UnsupportedOperationException(String.format("Type variables of '%s' cannot be resolved", type.name))
            }
            return fromClassWithGenerics(reflectMaid, type, emptyMap())
        }

        @JvmStatic
        fun fromClassWithGenerics(reflectMaid: ReflectMaid,
                                  type: Class<*>,
                                  typeParameters: Map<TypeVariableName, ResolvedType>): ClassType {
            NotNullValidator.validateNotNull(type, "type")
            NotNullValidator.validateNotNull(typeParameters, "typeParameters")
            if (type.isArray) {
                throw UnsupportedOperationException()
            }
            return ClassType(type, typeParameters, reflectMaid)
        }
    }
}

class Cached<T>(val supplier: () -> T) {
    private var cached: T? = null

    fun get(): T {
        if (cached == null) {
            cached = supplier.invoke()
        }
        return cached!!
    }

    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}
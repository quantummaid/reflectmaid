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
package de.quantummaid.reflectmaid.resolvedtype

import de.quantummaid.reflectmaid.GenericType
import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.RawClass
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.ThirdPartyAnnotation.Companion.thirdPartyAnnotation
import de.quantummaid.reflectmaid.TypeVariableName
import de.quantummaid.reflectmaid.languages.Language
import de.quantummaid.reflectmaid.languages.Language.Companion.JAVA
import de.quantummaid.reflectmaid.languages.Language.Companion.KOTLIN
import de.quantummaid.reflectmaid.queries.QueryPath
import de.quantummaid.reflectmaid.resolvedtype.UnresolvableTypeVariableException.Companion.unresolvableTypeVariableException
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor.Companion.resolveConstructors
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField.Companion.resolvedFields
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod.Companion.resolveMethodsWithResolvableTypeVariables
import java.lang.reflect.Modifier
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

class ClassType(
    private val raw: RawClass,
    private val typeParameters: Map<TypeVariableName, ResolvedType>,
    private val reflectMaid: ReflectMaid
) : ResolvedType {
    private val typeParametersList = Cached {
        raw
            .typeParameters()
            .map { TypeVariableName.typeVariableName(it) }
            .map { typeParameters[it]!! }
    }
    private val methods = Cached { resolveMethodsWithResolvableTypeVariables(reflectMaid, this, raw, language()) }
    private val constructors = Cached { resolveConstructors(reflectMaid, this, raw) }
    private val fields = Cached { resolvedFields(reflectMaid, this, raw) }
    private val sealedSubclasses = Cached { resolveSealedSubclasses(this, reflectMaid) }
    private val directSuperClass = NullableCached {
        raw.genericSuperType()
            ?.let { fromReflectionType<Any>(it, this) }
            ?.let { reflectMaid.resolve(it) }
    }
    private val directInterfaces = Cached {
        raw.genericInterfaces()
            .map { fromReflectionType<Any>(it, this) }
            .map { reflectMaid.resolve(it) }
    }
    private val isPublic = Cached {
        val modifiers = raw.modifiers()
        Modifier.isPublic(modifiers)
    }
    private val isAbstract = Cached {
        if (raw.isPrimitive()) {
            false
        } else {
            Modifier.isAbstract(raw.modifiers())
        }
    }
    private val isStatic = Cached {
        val modifiers = raw.modifiers()
        Modifier.isStatic(modifiers)
    }
    private val isInnerClass = Cached {
        raw.enclosingClass() != null
    }
    private val language = Cached { determineLanguage(this) }

    private val description = IndexedCached<Language, String> { language ->
        if (typeParameters.isEmpty()) {
            raw.name()
        } else {
            val parametersString = typeParameters()
                .joinToString(separator = ", ", prefix = "<", postfix = ">") { it.description(language) }
            raw.name() + parametersString
        }
    }

    private val simpleDescription = IndexedCached<Language, String> { language ->
        if (typeParameters.isEmpty()) {
            raw.simpleName()
        } else {
            val parametersString = typeParameters()
                .joinToString(separator = ", ", prefix = "<", postfix = ">") { it.simpleDescription(language) }
            raw.simpleName() + parametersString
        }
    }

    fun typeParameter(name: TypeVariableName): ResolvedType {
        require(typeParameters.containsKey(name)) { "No type parameter with the name: ${name.name}" }
        return typeParameters[name]!!
    }

    fun resolveTypeVariable(name: TypeVariableName): ResolvedType {
        if (!typeParameters.containsKey(name)) {
            throw unresolvableTypeVariableException(name)
        }
        return typeParameters[name]!!
    }

    fun cachedMethods(): List<ResolvedMethod> {
        return methods.cached() ?: emptyList()
    }

    fun cachedConstructors(): List<ResolvedConstructor> {
        return constructors.cached() ?: emptyList()
    }

    fun cachedFields(): List<ResolvedField> {
        return fields.cached() ?: emptyList()
    }

    override fun typeParameters() = typeParametersList.get()
    override fun methods() = methods.get()
    override fun constructors() = constructors.get()
    override fun fields() = fields.get()
    override fun sealedSubclasses() = sealedSubclasses.get()
    override fun directSuperClass() = directSuperClass.get()
    override fun directInterfaces() = directInterfaces.get()
    override fun language() = language.get()
    override fun description(language: Language) = description.get(language)
    override fun simpleDescription(language: Language) = simpleDescription.get(language)
    override fun isPublic() = isPublic.get()
    override fun isAbstract() = isAbstract.get()
    override fun isInterface() = raw.isInterface()
    override fun isAnonymousClass() = raw.isAnonymousClass()
    override fun isLocalClass() = raw.isLocalClass()
    override fun isAnnotation() = raw.isAnnotation()
    override fun isWildcard() = false
    override fun assignableType() = raw.wrappedClass()
    override fun isStatic() = isStatic.get()
    override fun isInnerClass() = isInnerClass.get()

    companion object {
        fun fromClassWithoutGenerics(
            reflectMaid: ReflectMaid,
            raw: RawClass
        ): ClassType {
            if (raw.isArray()) {
                throw UnsupportedOperationException()
            }
            if (raw.typeParameters().isNotEmpty()) {
                throw UnsupportedOperationException("Type variables of '${raw.name()}' cannot be resolved")
            }
            return fromClassWithGenerics(reflectMaid, raw, emptyMap())
        }

        fun fromClassWithGenerics(
            reflectMaid: ReflectMaid,
            raw: RawClass,
            typeParameters: Map<TypeVariableName, ResolvedType>
        ): ClassType {
            if (raw.isArray()) {
                throw UnsupportedOperationException()
            }
            return ClassType(raw, typeParameters, reflectMaid)
        }
    }

    override fun <T> query(path: QueryPath<T>, reason: String?): T {
        return path.extract(this, reason, reflectMaid)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other !is ClassType) {
            return false
        }
        if (other.reflectMaid != reflectMaid) {
            throw ComparingTypesOfDifferentReflectMaidsException(this, other)
        }
        if (other.raw != raw) {
            return false
        }
        return other.typeParameters == typeParameters
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }
}

private val KOTLIN_METADATA = thirdPartyAnnotation("kotlin.Metadata")

private fun determineLanguage(resolvedType: ResolvedType): Language {
    return if (isKotlinClass(resolvedType)) {
        KOTLIN
    } else {
        JAVA
    }
}

private fun isKotlinClass(resolvedType: ResolvedType): Boolean {
    return KOTLIN_METADATA.isAnnotatedWith(resolvedType)
}

private fun resolveSealedSubclasses(classType: ClassType, reflectMaid: ReflectMaid): List<ResolvedType> {
    return if (isKotlinClass(classType)) {
        val kotlinClass = Reflection.createKotlinClass(classType.assignableType())
        @Suppress("UNCHECKED_CAST", "NO_REFLECTION_IN_CLASS_PATH")
        (kotlinClass.sealedSubclasses as List<KClass<Any>>)
            .map { GenericType.genericType(it) }
            .map { reflectMaid.resolve(it) }
    } else {
        emptyList()
    }
}

class UnresolvableTypeVariableException private constructor(message: String) : RuntimeException(message) {
    companion object {
        fun unresolvableTypeVariableException(variableName: TypeVariableName): UnresolvableTypeVariableException {
            val message = "No type variable with name '${variableName.name}'"
            return UnresolvableTypeVariableException(message)
        }
    }
}

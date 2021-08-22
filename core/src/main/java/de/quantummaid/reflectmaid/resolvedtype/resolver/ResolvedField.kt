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

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.RawClass
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.languages.Language
import de.quantummaid.reflectmaid.resolvedtype.Cached
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.NullableCached
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

data class ResolvedField(
    val name: String,
    val type: ResolvedType,
    val declaringType: ResolvedType,
    val field: Field,
    val reflectMaid: ReflectMaid
) {
    private val isPublic = Cached {
        val modifiers = this.field.modifiers
        Modifier.isPublic(modifiers)
    }
    private val isFinal = Cached {
        val modifiers = this.field.modifiers
        Modifier.isFinal(modifiers)
    }
    private val isStatic = Cached {
        val modifiers = this.field.modifiers
        Modifier.isStatic(modifiers)
    }
    private val isTransient = Cached {
        val modifiers = this.field.modifiers
        Modifier.isTransient(modifiers)
    }
    private val description = Cached {
        val joiner = StringJoiner(" ")
        val modifiers = field.modifiers
        if (isPublic()) {
            joiner.add("public")
        }
        if (Modifier.isProtected(modifiers)) {
            joiner.add("protected")
        }
        if (Modifier.isPrivate(modifiers)) {
            joiner.add("private")
        }
        if (isStatic()) {
            joiner.add("static")
        }
        if (isTransient()) {
            joiner.add("transient")
        }
        if (Modifier.isFinal(modifiers)) {
            joiner.add("final")
        }
        val typeDescription = type.simpleDescription()
        joiner.add(typeDescription)
        joiner.add(name)
        joiner.toString()
    }
    private val getter = Cached { reflectMaid.executorFactory.createFieldGetter(this) }
    private val setter = Cached { reflectMaid.executorFactory.createFieldSetter(this) }
    private val getAccessor = NullableCached {
        if (declaringType.language() != Language.KOTLIN) {
            null
        } else {
            val accessorName = kotlinGetAccessorFunctionName()
            declaringType.methods()
                .filter { it.name == accessorName }
                .filter { it.isPublic() }
                .filter { it.returnType == type }
                .find { it.parameters.isEmpty() }
        }
    }

    private val setAccessor = NullableCached {
        if (declaringType.language() != Language.KOTLIN) {
            null
        } else {
            val accessorName = kotlinSetAccessorFunctionName()
            declaringType.methods()
                .asSequence()
                .filter { it.name == accessorName }
                .filter { it.isPublic() }
                .filter { it.returnType == null }
                .filter { it.parameters.size == 1 }
                .find { it.parameters[0].type == type }
        }
    }

    fun isPublic() = isPublic.get()
    fun isStatic() = isStatic.get()
    fun isFinal() = isFinal.get()
    fun isTransient() = isTransient.get()
    fun describe() = description.get()
    fun createGetter() = getter.get()
    fun createSetter() = setter.get()
    fun kotlinGetAccessor() = getAccessor.get()
    fun kotlinSetAccessor() = setAccessor.get()

    /*
    https://kotlinlang.org/docs/java-to-kotlin-interop.html#properties
     */
    fun kotlinGetAccessorFunctionName(): String {
        if (name.startsWith("is")) {
            return name
        }
        val firstChar = name[0]
        return "get" + name.replaceFirst(firstChar, firstChar.toUpperCase())
    }

    /*
    https://kotlinlang.org/docs/java-to-kotlin-interop.html#properties
    */
    fun kotlinSetAccessorFunctionName(): String {
        if (name.startsWith("is")) {
            return name.replaceFirst("is", "set")
        }
        val firstChar = name[0]
        return "set" + name.replaceFirst(firstChar, firstChar.toUpperCase())
    }

    companion object {
        fun resolvedFields(
            reflectMaid: ReflectMaid,
            fullType: ClassType,
            raw: RawClass
        ): List<ResolvedField> {
            return raw.declaredFields()
                .filter { !it.isSynthetic }
                .map {
                    val resolved = reflectMaid.resolve(fromReflectionType<Any>(it.genericType, fullType))
                    ResolvedField(it.name, resolved, fullType, it, reflectMaid)
                }
        }
    }
}
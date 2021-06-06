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
import de.quantummaid.reflectmaid.Getter
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.Setter
import de.quantummaid.reflectmaid.resolvedtype.Cached
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

data class ResolvedField(val name: String,
                         val type: ResolvedType,
                         val declaringType: ResolvedType,
                         val field: Field,
                         val reflectMaid: ReflectMaid) {
    private val getter: Cached<Getter> = Cached { reflectMaid.executorFactory.createFieldGetter(this) }
    private val setter: Cached<Setter> = Cached { reflectMaid.executorFactory.createFieldSetter(this) }

    val isPublic: Boolean
        get() {
            val modifiers = this.field.modifiers
            return Modifier.isPublic(modifiers)
        }

    val isStatic: Boolean
        get() {
            val modifiers = this.field.modifiers
            return Modifier.isStatic(modifiers)
        }

    val isTransient: Boolean
        get() {
            val modifiers = this.field.modifiers
            return Modifier.isTransient(modifiers)
        }

    fun describe(): String {
        val joiner = StringJoiner(" ")
        val modifiers = field.modifiers
        if (isPublic) {
            joiner.add("public")
        }
        if (Modifier.isProtected(modifiers)) {
            joiner.add("protected")
        }
        if (Modifier.isPrivate(modifiers)) {
            joiner.add("private")
        }
        if (isStatic) {
            joiner.add("static")
        }
        if (isTransient) {
            joiner.add("transient")
        }
        if (Modifier.isFinal(modifiers)) {
            joiner.add("final")
        }
        val typeDescription = type.simpleDescription()
        joiner.add(typeDescription)
        joiner.add(name)
        return joiner.toString()
    }

    fun createGetter() = getter.get()

    fun createSetter() = setter.get()

    companion object {
        fun resolvedFields(reflectMaid: ReflectMaid,
                           fullType: ClassType): List<ResolvedField> {
            val type = fullType.assignableType()
            return type.declaredFields
                    .filter { !it.isSynthetic }
                    .map {
                        val resolved = reflectMaid.resolve(fromReflectionType<Any>(it.genericType, fullType))
                        ResolvedField(it.name, resolved, fullType, it, reflectMaid)
                    }
        }
    }
}
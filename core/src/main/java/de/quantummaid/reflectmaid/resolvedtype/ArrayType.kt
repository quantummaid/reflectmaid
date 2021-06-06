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

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.languages.Language
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithoutGenerics
import java.lang.reflect.Array

data class ArrayType(private val componentType: ResolvedType) : ResolvedType {

    fun componentType(): ResolvedType {
        return componentType
    }

    override fun simpleDescription(language: Language): String {
        val componentTypeDescription = componentType.simpleDescription(language)
        return language.array(componentTypeDescription)
    }

    override fun description(language: Language): String {
        val componentTypeDescription = componentType.description(language)
        return language.array(componentTypeDescription)
    }

    override val isAbstract: Boolean
        get() = false
    override val isInterface: Boolean
        get() = false
    override val isWildcard: Boolean
        get() = false
    override val isArray: Boolean
        get() = true

    override fun typeParameters(): List<ResolvedType> {
        return listOf(componentType)
    }

    override fun assignableType(): Class<*> {
        return Array.newInstance(componentType.assignableType(), 0).javaClass
    }

    companion object {
        @JvmStatic
        fun fromArrayClass(reflectMaid: ReflectMaid?,
                           clazz: Class<*>): ArrayType {
            if (!clazz.isArray) {
                throw UnsupportedOperationException()
            }
            val componentType: ResolvedType = fromClassWithoutGenerics(reflectMaid!!, clazz.componentType)
            return arrayType(componentType)
        }

        @JvmStatic
        fun arrayType(componentType: ResolvedType): ArrayType {
            return ArrayType(componentType)
        }
    }
}
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
import java.io.Serializable
import java.lang.reflect.Array

data class ArrayType(
    private val componentType: ResolvedType,
    private val reflectMaid: ReflectMaid
) : ResolvedType {
    private val assignableType = Cached { Array.newInstance(componentType.assignableType(), 0).javaClass }
    private val directSuperClass = Cached { reflectMaid.resolve(Any::class.java) }
    private val directInterfaces = Cached {
        listOf(Cloneable::class.java, Serializable::class.java)
            .map { reflectMaid.resolve(it) }
    }
    private val simpleDescription = IndexedCached<Language, String> {
        val componentTypeDescription = componentType.simpleDescription(it)
        it.array(componentTypeDescription)
    }

    private val description = IndexedCached<Language, String> {
        val componentTypeDescription = componentType.description(it)
        it.array(componentTypeDescription)
    }

    fun componentType() = componentType
    override fun simpleDescription(language: Language) = simpleDescription.get(language)
    override fun description(language: Language) = description.get(language)
    override fun isAbstract() = false
    override fun isInterface() = false
    override fun isWildcard() = false
    override fun isArray() = true
    override fun typeParameters() = listOf(componentType)
    override fun assignableType() = assignableType.get()
    override fun directSuperClass() = directSuperClass.get()
    override fun directInterfaces() = directInterfaces.get()
}
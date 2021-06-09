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

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.resolvedtype.Cached
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.RawClass
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedParameter.Companion.resolveParameters
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

data class ResolvedConstructor(
    val parameters: List<ResolvedParameter>,
    val declaringType: ResolvedType,
    val constructor: Constructor<*>,
    val reflectMaid: ReflectMaid
) {
    private val isPublic = Cached {
        val modifiers = constructor.modifiers
        Modifier.isPublic(modifiers)
    }
    private val description = Cached { constructor.toGenericString() }
    private val executor = Cached { reflectMaid.executorFactory.createConstructorExecutor(this) }

    fun isPublic() = isPublic.get()

    fun describe() = description.get()

    fun createExecutor() = executor.get()

    companion object {
        fun resolveConstructors(
            reflectMaid: ReflectMaid,
            fullType: ClassType,
            raw: RawClass
        ): List<ResolvedConstructor> {
            return raw.declaredConstructors()
                .filter { !it.isSynthetic }
                .map {
                    val parameters = resolveParameters(reflectMaid, it, fullType)
                    ResolvedConstructor(parameters, fullType, it, reflectMaid)
                }
        }
    }
}
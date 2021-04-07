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
package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import java.lang.reflect.AnnotatedElement
import java.util.stream.Collectors

class ThirdPartyAnnotation(private val fullyQualifiedNames: List<String>) {

    fun describe(): String {
        return fullyQualifiedNames.stream()
                .collect(Collectors.joining(", ", "[", "]"))
    }

    fun isAnnotatedWith(constructor: ResolvedConstructor): Boolean {
        val rawConstructor = constructor.constructor
        return isAnnotated(rawConstructor)
    }

    fun isAnnotatedWith(method: ResolvedMethod): Boolean {
        val rawMethod = method.method
        return isAnnotated(rawMethod)
    }

    fun isAnnotatedWith(type: ResolvedType): Boolean {
        val rawType = type.assignableType()
        return isAnnotated(rawType)
    }

    private fun isAnnotated(annotatedElement: AnnotatedElement): Boolean {
        val annotations = annotatedElement.annotations
        return annotations
                .map { it.annotationClass }
                .map { it.qualifiedName }
                .any { fullyQualifiedNames.contains(it) }
    }

    companion object {
        @JvmStatic
        fun thirdPartyAnnotation(vararg fullyQualifiedNames: String): ThirdPartyAnnotation {
            return ThirdPartyAnnotation(fullyQualifiedNames.toList())
        }
    }
}
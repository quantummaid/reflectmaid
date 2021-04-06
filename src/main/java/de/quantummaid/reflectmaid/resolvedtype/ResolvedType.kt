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

interface ResolvedType {
    fun assignableType(): Class<*>

    fun typeParameters(): List<ResolvedType>

    val isAbstract: Boolean

    val isInterface: Boolean

    val isPublic: Boolean
        get() = true

    val isAnonymousClass: Boolean
        get() = false

    val isInnerClass: Boolean
        get() = false

    val isLocalClass: Boolean
        get() = false

    val isStatic: Boolean
        get() = false

    val isAnnotation: Boolean
        get() = false

    val isArray: Boolean
        get() = false

    val isWildcard: Boolean

    fun description(): String

    fun simpleDescription(): String {
        return description()
    }

    fun sealedSubclasses(): List<ResolvedType> {
        return emptyList()
    }

    val isInstantiatable: Boolean
        get() {
            if (isInterface) {
                return false
            }
            if (isAbstract) {
                return false
            }
            return if (isWildcard) {
                false
            } else typeParameters().stream()
                    .allMatch { obj: ResolvedType -> obj.isInstantiatable }
        }
}
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

import de.quantummaid.reflectmaid.languages.Language

class WildcardedType : ResolvedType {
    override fun typeParameters(): List<ResolvedType> {
        return emptyList()
    }

    override fun isAbstract() = false
    override fun isInterface() = false
    override fun isWildcard() = true
    override fun description(language: Language) = language.wildcard()
    override fun assignableType() = Any::class.java
    override fun directSuperClass() = null
    override fun directInterfaces() = emptyList<ResolvedType>()

    override fun equals(other: Any?): Boolean {
        return other is WildcardedType
    }

    override fun hashCode(): Int {
        return 1
    }
}
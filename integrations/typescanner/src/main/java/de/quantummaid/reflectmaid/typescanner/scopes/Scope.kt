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
package de.quantummaid.reflectmaid.typescanner.scopes

import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import java.util.ArrayList
import java.util.stream.Collectors

data class Scope(private val scope: List<TypeIdentifier>) {

    fun childScope(subScope: TypeIdentifier): Scope {
        val newScope = ArrayList(scope)
        newScope.add(subScope)
        return Scope(newScope)
    }

    fun render(): String {
        return scope.stream()
            .map { it.simpleDescription() }
            .collect(Collectors.joining("/", "/", ""))
    }

    fun size(): Int {
        return scope.size
    }

    fun containsElement(type: TypeIdentifier): Boolean {
        return scope.contains(type)
    }

    operator fun contains(other: Scope): Boolean {
        if (size() > other.size()) {
            return false
        }
        for (i in scope.indices) {
            val thisPart = scope[i]
            val otherPart = other.scope[i]
            if (thisPart != otherPart) {
                return false
            }
        }
        return true
    }

    companion object {
        @JvmStatic
        fun rootScope(): Scope {
            return Scope(emptyList())
        }
    }
}
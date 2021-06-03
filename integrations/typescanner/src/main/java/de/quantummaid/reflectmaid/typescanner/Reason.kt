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
package de.quantummaid.reflectmaid.typescanner

data class Reason(
    private val reason: String,
    private val parent: TypeIdentifier?
) {

    fun reason() = reason

    fun render(subReasonProvider: SubReasonProvider): List<String> {
        return renderRecursive(subReasonProvider, ArrayList())
    }

    private fun renderRecursive(
        subReasonProvider: SubReasonProvider,
        alreadyVisitedReasons: MutableList<Reason>
    ): List<String> {
        if (parent == null) {
            return listOf(reason)
        }
        val parentName = parent.description()
        if (alreadyVisitedReasons.contains(this)) {
            return listOf("$parentName...")
        }
        alreadyVisitedReasons.add(this)
        val parentReasons = subReasonProvider.reasonsFor(parent)
        return parentReasons
            .flatMap { it.renderRecursive(subReasonProvider, alreadyVisitedReasons) }
            .map { "$parentName -> $it" }
    }

    companion object {
        @JvmStatic
        fun manuallyAdded(): Reason {
            return reason("manually added")
        }

        @JvmStatic
        fun becauseOf(parent: TypeIdentifier): Reason {
            return Reason("because of ${parent.description()}", parent)
        }

        @JvmStatic
        fun reason(reason: String): Reason {
            return Reason(reason, null)
        }
    }
}

fun interface SubReasonProvider {
    fun reasonsFor(typeIdentifier: TypeIdentifier): List<Reason>
}
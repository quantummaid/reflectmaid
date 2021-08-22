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
package de.quantummaid.reflectmaid.queries

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType

class QueryNotFoundException private constructor(override val message: String) : RuntimeException() {

    companion object {
        fun queryNotFoundBecauseNoFollowUpException(
            queryPath: QueryPath<*>,
            currentElement: QueryPathElement<*, *>,
            currentMatch: List<QueryResult<*>>,
            lastMatch: QueryResult<*>,
            reflectMaid: ReflectMaid,
            reason: String?
        ): QueryNotFoundException {
            val renderedCurrent = currentElement.render(reflectMaid)
            val lastDescription = lastMatch.describe()
            val message = "unable to continue matching query [${renderedCurrent}] from $lastDescription"
            return queryNotFoundException(message, queryPath, currentMatch, reflectMaid, reason)
        }

        fun queryNotFoundException(
            queryPath: QueryPath<*>,
            currentElement: QueryPathElement<*, *>,
            currentMatch: List<QueryResult<*>>,
            currentType: ResolvedType,
            reflectMaid: ReflectMaid,
            reason: String?
        ): QueryNotFoundException {
            val renderedCurrent = currentElement.render(reflectMaid)
            val renderedType = renderClassHierarchical(currentType, reflectMaid)
            val message = "unable to find query [${renderedCurrent}] in type ${currentType.description()}\n" +
                    renderedType
            return queryNotFoundException(message, queryPath, currentMatch, reflectMaid, reason)
        }

        private fun renderClassHierarchical(resolvedType: ResolvedType, reflectMaid: ReflectMaid): String {
            val base = renderClass(resolvedType, reflectMaid)
            val inherited = resolvedType.allSupertypes().joinToString(separator = "\n") {
                val rendered = renderClass(it, reflectMaid)
                "inherited from ${it.description()}:\n" +
                        rendered
            }
            return "$base\n$inherited"
        }

        private fun renderClass(resolvedType: ResolvedType, reflectMaid: ReflectMaid): String {
            val fields = resolvedType.fields()
            val fieldDescriptions = if (fields.isEmpty()) {
                ""
            } else {
                fields.joinToString(separator = "\n", postfix = "\n") { it.describe() }
            }
            val methods = resolvedType.methods()
            val methodDescriptions = if (methods.isEmpty()) {
                ""
            } else {
                methods.joinToString(separator = "\n", postfix = "\n") { it.describe() }
            }

            return "fields:\n${fieldDescriptions}" +
                    "methods:\n${methodDescriptions}"
        }

        private fun queryNotFoundException(
            message: String,
            queryPath: QueryPath<*>,
            currentMatch: List<QueryResult<*>>,
            reflectMaid: ReflectMaid,
            reason: String?
        ): QueryNotFoundException {
            val wholeMatch = queryPath.elements
                .mapIndexed { index, element ->
                    val rendered = element.render(reflectMaid)
                    val match = currentMatch.elementAtOrNull(index)
                        ?.describe()
                        ?: "???"
                    "#$index: $rendered -> $match"
                }
                .joinToString(separator = "\n")
            val reasonString = reason
                ?.let { "reason: $reason\n" }
                ?: ""
            val wholeMessage = "$message\n\n" +
                    reasonString +
                    "whole query:\n$wholeMatch"
            return QueryNotFoundException(wholeMessage)
        }
    }
}
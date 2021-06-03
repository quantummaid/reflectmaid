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
package de.quantummaid.reflectmaid.typescanner.states

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

class DetectionResult<T>(private val result: T?, private val reasonsForFailure: List<String>) {
    fun ifSuccess(consumer: Consumer<T>) {
        if (isSuccess()) {
            consumer.accept(result!!)
        }
    }

    fun isFailure() = !reasonsForFailure.isEmpty()

    fun isSuccess() = !isFailure()

    fun reasonForFailure(): String {
        return reasonsForFailure.joinToString(separator = "\n", prefix = "[", postfix = "]")
    }

    fun result(): T {
        return result!!
    }

    fun <X> map(mapper: Function<T, X>): DetectionResult<X> {
        if (isFailure()) {
            return this as DetectionResult<X>
        }
        val mapped = mapper.apply(result!!)
        return success(mapped)
    }

    fun <X> mapWithNull(mapper: Function<T?, X>): DetectionResult<X> {
        val mapped = mapper.apply(result)
        return DetectionResult(mapped, reasonsForFailure)
    }

    fun <X> flatMap(mapper: Function<T, DetectionResult<X>>): DetectionResult<X> {
        return if (isFailure()) {
            this as DetectionResult<X>
        } else mapper.apply(result!!)
    }

    companion object {
        @JvmStatic
        fun <A, B, C> combine(
            a: DetectionResult<A>,
            b: DetectionResult<B>,
            combinator: BiFunction<A, B, C>
        ): DetectionResult<C> {
            if (!a.isFailure() && !b.isFailure()) {
                val combination = combinator.apply(a.result(), b.result())
                return success(combination)
            }
            val combinedReasons: MutableList<String> = ArrayList()
            combinedReasons.addAll(a.reasonsForFailure)
            combinedReasons.addAll(b.reasonsForFailure)
            return failure(combinedReasons)
        }

        @JvmStatic
        fun <T> success(result: T): DetectionResult<T> {
            return DetectionResult(result, emptyList())
        }

        @JvmStatic
        fun <T> failure(reasonForFailure: String): DetectionResult<T> {
            return failure(listOf(reasonForFailure))
        }

        @JvmStatic
        fun <T> failure(reasonsForFailure: List<String>): DetectionResult<T> {
            return DetectionResult(null, reasonsForFailure)
        }

        @JvmStatic
        fun <T> followUpFailure(vararg detectionResults: DetectionResult<*>): DetectionResult<T> {
            val combinedReasons: MutableList<String> = ArrayList()
            for (result in detectionResults) {
                require(result.isFailure()) { "Can only follow up on failures" }
                combinedReasons.addAll(result.reasonsForFailure)
            }
            return failure(combinedReasons)
        }
    }
}
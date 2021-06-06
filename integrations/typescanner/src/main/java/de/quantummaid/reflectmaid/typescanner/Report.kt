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
package de.quantummaid.reflectmaid.typescanner

import java.util.*

class Report<T>(private val result: CollectionResult<T>?, private val errorMessage: String?) {

    fun isSuccess() = Objects.isNull(errorMessage) && !Objects.isNull(result)

    fun isEmpty() = Objects.isNull(errorMessage) && Objects.isNull(result)

    fun result(): CollectionResult<T>? {
        return result
    }

    fun errorMessage(): String {
        return errorMessage!!
    }

    companion object {
        fun <T> success(result: CollectionResult<T>): Report<T> {
            return Report(result, null)
        }

        fun <T> failure(result: CollectionResult<T>, errorMessage: String): Report<T> {
            return Report(result, errorMessage)
        }

        fun <T> empty(): Report<T> {
            return Report(null, null)
        }
    }
}
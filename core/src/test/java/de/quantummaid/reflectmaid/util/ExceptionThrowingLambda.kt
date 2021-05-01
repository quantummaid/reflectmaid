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
package de.quantummaid.reflectmaid.util

import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat

fun interface ExceptionThrowingLambda {
    @Throws(Exception::class)
    fun run()

    companion object {
        @JvmStatic
        fun withException(runnable: ExceptionThrowingLambda): Exception {
            return withException<java.lang.Exception>(runnable)
        }
    }
}

inline fun <reified T : java.lang.Exception> withException(runnable: ExceptionThrowingLambda): T {
    var exception: Exception? = null
    try {
        runnable.run()
    } catch (e: Exception) {
        exception = e
    }
    assertThat(exception, notNullValue())
    assertThat(exception, instanceOf(T::class.java))
    return (exception as T)
}
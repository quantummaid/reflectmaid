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

class IndexedCached<I, T>(private val supplier: (I) -> T) {
    private val map = mutableMapOf<I, T>()

    fun get(index: I): T {
        return map.computeIfAbsent(index, supplier)
    }
}

private class NullableCacheValue<T>(val value: T?)

class NullableCached<T>(private val supplier: () -> T?) {
    private val delegate: Cached<NullableCacheValue<T>> = Cached {
        val value = supplier.invoke()
        NullableCacheValue(value)
    }

    fun get() = delegate.get().value
}

class Cached<T>(private val supplier: () -> T) {
    private var cached: T? = null

    fun get(): T {
        if (cached == null) {
            cached = supplier.invoke()
        }
        return cached!!
    }

    fun cached(): T? {
        return cached
    }

    override fun equals(other: Any?): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return 0
    }
}

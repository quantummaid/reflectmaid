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

import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod

sealed class QueryResult<T>(val previous: QueryResult<*>?) {
    abstract fun createGetter(): TypedGetter<T>
    abstract fun createSetter(): TypedSetter<T>
    abstract fun describe(): String
}

class QueriedField<T>(val resolvedField: ResolvedField, previous: QueryResult<*>?) : QueryResult<T>(previous) {

    override fun createGetter(): TypedGetter<T> {
        val previousGetter = previous?.createGetter()
        return TypedGetter.getterFromField(resolvedField, previousGetter)
    }

    override fun createSetter(): TypedSetter<T> {
        val previousGetter = previous?.createGetter()
        return TypedSetter.setterFromField(resolvedField, previousGetter)
    }

    override fun describe(): String {
        return resolvedField.describe()
    }
}

class QueriedMethod<T>(val resolvedMethod: ResolvedMethod, previous: QueryResult<*>?) : QueryResult<T>(previous) {
    override fun createGetter(): TypedGetter<T> {
        val previousGetter = previous?.createGetter()
        return TypedGetter.getterFromMethod(resolvedMethod, previousGetter)
    }

    override fun createSetter(): TypedSetter<T> {
        val previousGetter = previous?.createGetter()
        return TypedSetter.setterFromMethod(resolvedMethod, previousGetter)
    }

    override fun describe(): String {
        return resolvedMethod.describe()
    }
}
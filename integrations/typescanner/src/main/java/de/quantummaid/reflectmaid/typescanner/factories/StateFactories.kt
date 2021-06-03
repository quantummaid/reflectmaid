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
package de.quantummaid.reflectmaid.typescanner.factories

import de.quantummaid.reflectmaid.typescanner.Context
import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition
import de.quantummaid.reflectmaid.typescanner.states.detected.Unreasoned

fun interface StateFactory<T> {
    fun create(type: TypeIdentifier, context: Context<T>): StatefulDefinition<T>?
}

class UndetectedFactory<T> : StateFactory<T> {
    override fun create(type: TypeIdentifier, context: Context<T>): StatefulDefinition<T> {
        return Unreasoned(context)
    }
}

class StateFactories<T>(private val stateFactories: List<StateFactory<T>>) {

    fun createState(type: TypeIdentifier, context: Context<T>): StatefulDefinition<T> {
        for (stateFactory in stateFactories) {
            val statefulDefinition = stateFactory.create(type, context)
            if (statefulDefinition != null) {
                return statefulDefinition
            }
        }
        throw UnsupportedOperationException("This should never happen")
    }
}
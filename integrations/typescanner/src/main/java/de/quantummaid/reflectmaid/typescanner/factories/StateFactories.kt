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
package de.quantummaid.reflectmaid.typescanner.factories

import de.quantummaid.reflectmaid.typescanner.Context
import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition
import de.quantummaid.reflectmaid.typescanner.states.detected.Unreasoned

interface StateFactory<T> {
    fun applies(type: TypeIdentifier): Boolean

    fun create(type: TypeIdentifier, context: Context<T>)
}

class UndetectedFactory<T> : StateFactory<T> {

    override fun applies(type: TypeIdentifier) = true

    override fun create(type: TypeIdentifier, context: Context<T>) {
        // do nothing
    }
}

class StateFactories<T>(
    private val stateFactories: Map<Scope, List<StateFactory<T>>>,
    private val defaultFactory: StateFactory<T>
) {

    fun createBetterState(
        type: TypeIdentifier,
        scope: Scope,
        currentScope: Scope,
        contextProvider: (Scope) -> Context<T>
    ): StatefulDefinition<T>? {
        val relevantScopes = stateFactories.keys
            .filter { it.contains(scope) }
            .filter { it.size() > currentScope.size() }
            .sortedByDescending { it.size() }
        relevantScopes.forEach { actualScope ->
            val factory = stateFactories[actualScope]!!.firstOrNull { it.applies(type) }
            if (factory != null) {
                return createState(factory, actualScope, contextProvider)
            }
        }
        return null
    }

    fun createState(type: TypeIdentifier, scope: Scope, contextProvider: (Scope) -> Context<T>): StatefulDefinition<T> {
        val relevantScopes = stateFactories.keys
            .filter { it.contains(scope) }
            .sortedByDescending { it.size() }
        relevantScopes.forEach { actualScope ->
            val factory = stateFactories[actualScope]!!.firstOrNull { it.applies(type) }
            if (factory != null) {
                return createState(factory, actualScope, contextProvider)
            }
        }
        return createState(defaultFactory, scope, contextProvider)
    }

    private fun createState(stateFactory: StateFactory<T>,
                            actualScope: Scope,
                            contextProvider: (Scope) -> Context<T>): StatefulDefinition<T> {
        val context = contextProvider.invoke(actualScope)
        stateFactory.create(context.type, context)
        return Unreasoned(context)
    }
}
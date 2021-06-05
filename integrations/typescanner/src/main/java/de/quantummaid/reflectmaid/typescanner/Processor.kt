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

import de.quantummaid.reflectmaid.typescanner.factories.StateFactories
import de.quantummaid.reflectmaid.typescanner.factories.StateFactory
import de.quantummaid.reflectmaid.typescanner.log.StateLog
import de.quantummaid.reflectmaid.typescanner.log.StateLogBuilder
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementName
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.signals.DetectSignal
import de.quantummaid.reflectmaid.typescanner.signals.ResolveSignal
import de.quantummaid.reflectmaid.typescanner.signals.Signal
import de.quantummaid.reflectmaid.typescanner.states.Detector
import de.quantummaid.reflectmaid.typescanner.states.RequirementsDescriber
import de.quantummaid.reflectmaid.typescanner.states.Resolver
import java.util.*

class Processor<T>(
    private var states: States<T>,
    private val pendingSignals: Queue<Signal<T>>,
    private val log: StateLogBuilder<T>
) {
    fun dispatch(signal: Signal<T>) {
        pendingSignals.add(signal)
    }

    fun collect(
        detector: Detector<T>,
        resolver: Resolver<T>,
        onError: OnCollectionError<T>,
        requirementsDescriber: RequirementsDescriber
    ): Map<TypeIdentifier, Map<Scope, CollectionResult<T>>> {
        resolveRecursively(detector, resolver, requirementsDescriber)
        val reports = states.collect(requirementsDescriber)
        val all: MutableMap<TypeIdentifier, MutableMap<Scope, CollectionResult<T>>> = HashMap(reports.size)
        val definitions: MutableMap<TypeIdentifier, MutableMap<Scope, CollectionResult<T>>> = HashMap(reports.size)
        val failures: MutableMap<TypeIdentifier, MutableMap<Scope, Report<T>>> = LinkedHashMap()
        reports.forEach { (type, reportByScope) ->
            all[type] = LinkedHashMap()
            val definitionsByScope = LinkedHashMap<Scope, CollectionResult<T>>()
            val failuresByScope = LinkedHashMap<Scope, Report<T>>()
            reportByScope.forEach { (scope, report) ->
                val result = report.result()!!
                all[type]!![scope] = result
                if (report.isSuccess()) {
                    definitionsByScope[scope] = result
                } else {
                    failuresByScope[scope] = report
                }
            }
            if (definitionsByScope.isNotEmpty()) {
                definitions[type] = definitionsByScope
            }
            if (failuresByScope.isNotEmpty()) {
                failures[type] = failuresByScope
            }
        }
        if (failures.isNotEmpty()) {
            onError.onCollectionError(all, log.build(), failures)
        }
        return definitions
    }

    private fun resolveRecursively(
        detector: Detector<T>,
        resolver: Resolver<T>,
        requirementsDescriber: RequirementsDescriber
    ) {
        while (!pendingSignals.isEmpty()) {
            val signal = pendingSignals.remove()
            states = states.apply(signal, this, log)
        }
        val detected = states.apply(DetectSignal(detector, requirementsDescriber), this, log)
        val resolved = detected.apply(ResolveSignal(resolver), this, log)
        states = resolved
        if (!pendingSignals.isEmpty()) {
            resolveRecursively(detector, resolver, requirementsDescriber)
        }
    }

    fun log(): StateLog<T> {
        return log.build()
    }

    companion object {
        @JvmStatic
        fun <T> processor(
            stateFactories: List<StateFactory<T>>,
            primaryRequirements: List<RequirementName>,
            secondaryRequirements: List<RequirementName>
        ): Processor<T> {
            val pendingSignals: Queue<Signal<T>> = LinkedList()
            val states = States.states(
                ArrayList(),
                StateFactories(stateFactories),
                primaryRequirements,
                secondaryRequirements
            )
            val log = StateLogBuilder<T>()
            return Processor(states, pendingSignals, log)
        }
    }
}
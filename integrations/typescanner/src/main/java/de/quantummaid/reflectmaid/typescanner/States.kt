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
import de.quantummaid.reflectmaid.typescanner.log.LoggedState
import de.quantummaid.reflectmaid.typescanner.log.StateLogBuilder
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements.Companion.empty
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementName
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.signals.Signal
import de.quantummaid.reflectmaid.typescanner.states.RequirementsDescriber
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition
import java.util.stream.Collectors

class States<T>(
    private val stateFactories: StateFactories<T>,
    private val states: List<StatefulDefinition<T>>,
    private val primaryRequirements: List<RequirementName>,
    private val secondaryRequirements: List<RequirementName>
) {

    fun apply(signal: Signal<T>, processor: Processor<T>, stateLog: StateLogBuilder<T>): States<T> {
        val newStates = apply(signal, processor)
        stateLog.log(signal, newStates.dumpForLogging())
        return newStates
    }

    private fun apply(signal: Signal<T>, processor: Processor<T>): States<T> {
        val target = signal.target()
        return if (target == null) {
            val newStates = states.stream()
                .map { definition: StatefulDefinition<T>? ->
                    signal.handleState(
                        definition!!
                    )
                }
                .collect(Collectors.toList())
            States(stateFactories, newStates, primaryRequirements, secondaryRequirements)
        } else {
            val newStates: MutableList<StatefulDefinition<T>> = ArrayList(
                states
            )
            if (!contains(target.typeIdentifier, target.scope, newStates)) {
                val detectionRequirements = empty(primaryRequirements, secondaryRequirements)
                val context = Context<T>(target.typeIdentifier, target.scope, detectionRequirements) { processor.dispatch(it) }
                val state = stateFactories.createState(target.typeIdentifier, context)
                newStates.add(state)
            }
            newStates.replaceAll {
                if (it.matches(target.typeIdentifier, target.scope)) {
                    return@replaceAll signal.handleState(it)
                } else {
                    return@replaceAll it
                }
            }
            States(stateFactories, newStates, primaryRequirements, secondaryRequirements)
        }
    }

    fun collect(requirementsDescriber: RequirementsDescriber): Map<TypeIdentifier, Map<Scope, Report<T>>> {
        val reports: MutableMap<TypeIdentifier, MutableMap<Scope, Report<T>>> = HashMap()
        states.forEach {
            val report = it.getDefinition(requirementsDescriber)
            if (!report.isEmpty()) {
                val type = it.context.type
                val reportByScope = reports.computeIfAbsent(type) { LinkedHashMap() }
                reportByScope[it.scope()] = report
            }
        }
        return reports
    }

    private fun contains(
        type: TypeIdentifier,
        scope: Scope,
        states: List<StatefulDefinition<T>>,
    ): Boolean {
        return states.any { it.matches(type, scope) }
    }

    private fun dumpForLogging(): List<LoggedState> {
        return states.map {
            val type = it.type()
            val detectionRequirements = it.context
                .detectionRequirements()
            LoggedState(type, it.javaClass, detectionRequirements)
        }
    }

    companion object {
        fun <T> states(
            initialDefinitions: List<StatefulDefinition<T>>,
            stateFactories: StateFactories<T>,
            primaryRequirements: List<RequirementName>,
            secondaryRequirements: List<RequirementName>
        ): States<T> {
            val states: List<StatefulDefinition<T>> = ArrayList(initialDefinitions)
            return States(stateFactories, states, primaryRequirements, secondaryRequirements)
        }
    }
}
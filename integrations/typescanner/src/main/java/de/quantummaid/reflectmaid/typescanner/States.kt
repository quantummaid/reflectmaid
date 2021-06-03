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
    fun addState(statefulDefinition: StatefulDefinition<T>): States<T> {
        require(!contains(statefulDefinition.type(), states)) {
            "state for type '${statefulDefinition.type().description()}' is already registered"
        }
        val newStates: MutableList<StatefulDefinition<T>> = ArrayList(states)
        newStates.add(statefulDefinition)
        return States(stateFactories, newStates, primaryRequirements, secondaryRequirements)
    }

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
            if (!contains(target, newStates)) {
                val detectionRequirements = empty(primaryRequirements, secondaryRequirements)
                val context = Context<T>(target, detectionRequirements) { processor.dispatch(it) }
                val state = stateFactories.createState(target, context)
                newStates.add(state)
            }
            newStates.replaceAll { statefulDefinition: StatefulDefinition<T> ->
                if (statefulDefinition.context.type() == target) {
                    return@replaceAll signal.handleState(statefulDefinition)
                } else {
                    return@replaceAll statefulDefinition
                }
            }
            States(stateFactories, newStates, primaryRequirements, secondaryRequirements)
        }
    }

    fun collect(requirementsDescriber: RequirementsDescriber): Map<TypeIdentifier, Report<T>> {
        val reports: MutableMap<TypeIdentifier, Report<T>> = HashMap()
        states.forEach{
            val report = it.getDefinition(requirementsDescriber)
            if (!report.isEmpty()) {
                val type = it.context.type()
                reports[type] = report
            }
        }
        return reports
    }

    private fun contains(
        type: TypeIdentifier,
        states: List<StatefulDefinition<T>>
    ): Boolean {
        return states.stream()
            .anyMatch { statefulDefinition: StatefulDefinition<T> -> statefulDefinition.context.type() == type }
    }

    private fun dumpForLogging(): List<LoggedState> {
        return states.stream()
            .map { statefulDefinition: StatefulDefinition<T> ->
                val type = statefulDefinition.type()
                val detectionRequirements = statefulDefinition.context
                    .detectionRequirements()
                LoggedState(type, statefulDefinition.javaClass, detectionRequirements)
            }
            .collect(Collectors.toList())
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
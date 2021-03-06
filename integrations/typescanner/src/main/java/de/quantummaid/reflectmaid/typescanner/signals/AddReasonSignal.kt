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
package de.quantummaid.reflectmaid.typescanner.signals

import de.quantummaid.reflectmaid.typescanner.Reason
import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementName
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.scopes.Scope.Companion.rootScope
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition

data class AddReasonSignal<T>(
    private val target: SignalTarget,
    private val requirement: RequirementName,
    private val reason: Reason
) : Signal<T> {

    companion object {
        @JvmStatic
        fun <T> addReasonSignal(
            targetType: TypeIdentifier,
            requirement: RequirementName,
            reason: Reason
        ): AddReasonSignal<T> {
            return addReasonSignal(targetType, rootScope(), requirement, reason)
        }

        @JvmStatic
        fun <T> addReasonSignal(
            targetType: TypeIdentifier,
            targetScope: Scope,
            requirement: RequirementName,
            reason: Reason
        ): AddReasonSignal<T> {
            val target = SignalTarget(targetType, targetScope)
            return AddReasonSignal(target, requirement, reason)
        }
    }

    override fun handleState(definition: StatefulDefinition<T>): StatefulDefinition<T> {
        return definition.changeRequirements { it.addReason(requirement, reason) }
    }

    override fun target(): SignalTarget {
        return target
    }

    override fun description(): String {
        return "add ${requirement.value()} to ${target.description()}"
    }
}
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

import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementsReducer
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.signals.Signal
import de.quantummaid.reflectmaid.typescanner.states.DetectionResult
import de.quantummaid.reflectmaid.typescanner.states.RequiredAction

fun interface Dispatcher<T> {
    fun dispatch(signal: Signal<T>)
}

class Context<T>(
    val type: TypeIdentifier,
    val scope: Scope,
    private var detectionRequirements: DetectionRequirements,
    private val dispatcher: Dispatcher<T>
) {
    private var manuallyConfiguredResult: T? = null
    private var detectionResult: DetectionResult<T>? = null

    fun dispatch(signal: Signal<T>) {
        dispatcher.dispatch(signal)
    }

    fun setDetectionResult(detectionResult: DetectionResult<T>) {
        this.detectionResult = detectionResult
    }

    fun detectionResult(): DetectionResult<T>? {
        return detectionResult
    }

    fun setManuallyConfiguredResult(manuallyConfiguredResult: T) {
        this.manuallyConfiguredResult = manuallyConfiguredResult
    }

    fun manuallyConfiguredResult(): T? {
        return manuallyConfiguredResult
    }

    fun detectionRequirements(): DetectionRequirements {
        return detectionRequirements
    }

    fun changeRequirements(reducer: RequirementsReducer): RequiredAction {
        val oldReaons: DetectionRequirements = detectionRequirements
        val newReasons: DetectionRequirements = reducer.reduce(oldReaons)
        detectionRequirements = newReasons
        if (detectionRequirements.isUnreasoned()) {
            return RequiredAction.unreasoned()
        }
        return if (oldReaons.hasChanged(newReasons)) {
            RequiredAction.requirementsChanged()
        } else {
            RequiredAction.nothingChanged()
        }
    }
}
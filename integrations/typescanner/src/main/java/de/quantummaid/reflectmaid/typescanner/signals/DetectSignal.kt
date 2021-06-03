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
package de.quantummaid.reflectmaid.typescanner.signals

import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.states.Detector
import de.quantummaid.reflectmaid.typescanner.states.RequirementsDescriber
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition

data class DetectSignal<T>(
    private val detector: Detector<T>,
    private val requirementsDescriber: RequirementsDescriber
) : Signal<T> {

    override fun handleState(definition: StatefulDefinition<T>): StatefulDefinition<T> {
        return definition.detect(detector, requirementsDescriber)
    }

    override fun target(): TypeIdentifier? {
        return null
    }

    override fun description(): String {
        return "detect"
    }
}
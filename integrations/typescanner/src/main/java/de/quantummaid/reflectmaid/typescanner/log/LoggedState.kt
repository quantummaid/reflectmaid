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
package de.quantummaid.reflectmaid.typescanner.log

import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements

class LoggedState(
    private val type: TypeIdentifier,
    private val state: Class<*>,
    private val detectionRequirements: DetectionRequirements
) {
    fun buildTypeDescription(): String {
        return type.description()
    }

    fun buildStateName(): String {
        return state.simpleName
    }

    fun buildDetectionRequirementReasons(): String {
        return detectionRequirements.summary()
    }

    fun dump(): String {
        return "${type.simpleDescription()}: $state (${detectionRequirements.summary()})"
    }
}
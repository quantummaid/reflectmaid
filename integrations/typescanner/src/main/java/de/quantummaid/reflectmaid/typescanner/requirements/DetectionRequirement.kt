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
package de.quantummaid.reflectmaid.typescanner.requirements

import de.quantummaid.reflectmaid.typescanner.Reason

class DetectionRequirement(
    private val name: RequirementName,
    private val reasons: List<Reason>,
    private val primary: Boolean
) {
    fun isRequired() = reasons.isNotEmpty()
    fun numberOfReasons() = reasons.size
    fun reasons() = reasons
    fun name() = name
    fun isPrimary() = primary

    fun addReason(reason: Reason): DetectionRequirement {
        val newReasons = ArrayList(reasons)
        newReasons.add(reason)
        return DetectionRequirement(name, newReasons, primary)
    }

    fun removeReason(reason: Reason): DetectionRequirement {
        val newReasons = ArrayList(reasons)
        newReasons.remove(reason)
        return DetectionRequirement(name, newReasons, primary)
    }

    companion object {
        fun primaryRequirement(name: RequirementName): DetectionRequirement {
            return detectionRequirement(name, true)
        }

        fun secondaryRequirement(name: RequirementName): DetectionRequirement {
            return detectionRequirement(name, false)
        }

        private fun detectionRequirement(
            name: RequirementName,
            primary: Boolean
        ): DetectionRequirement {
            return DetectionRequirement(name, ArrayList(), primary)
        }
    }
}
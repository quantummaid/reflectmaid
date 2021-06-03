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
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirement.Companion.primaryRequirement
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirement.Companion.secondaryRequirement

class DetectionRequirements(private val detectionRequirements: Map<RequirementName, DetectionRequirement>) {

    fun requires(requirement: RequirementName): Boolean {
        val detectionRequirement = detectionRequirements[requirement]!!
        return detectionRequirement.isRequired()
    }

    fun addReason(requirement: RequirementName, reason: Reason): DetectionRequirements {
        return reduce(requirement) { it.addReason(reason) }
    }

    fun removeReason(reason: Reason): DetectionRequirements {
        val newRequirements = detectionRequirements
            .map { (name, requirement) -> name to requirement.removeReason(reason) }
            .toMap()
        return DetectionRequirements(newRequirements)
    }

    private fun reduce(
        requirement: RequirementName,
        reducer: (DetectionRequirement) -> DetectionRequirement
    ): DetectionRequirements {
        val newRequirements = LinkedHashMap(detectionRequirements)
        val detectionRequirement = newRequirements[requirement]!!
        val changedRequirement = reducer.invoke(detectionRequirement)
        newRequirements[requirement] = changedRequirement
        return DetectionRequirements(newRequirements)
    }

    fun hasChanged(old: DetectionRequirements): Boolean {
        val currentDetectionRequirements = currentRequirements()
        val oldDetectionRequirements = old.currentRequirements()
        return currentDetectionRequirements != oldDetectionRequirements
    }

    fun currentRequirements(): Map<RequirementName, Boolean> {
        return detectionRequirements
            .map { (name, requirement) -> name to requirement.isRequired() }
            .toMap()
    }

    fun isUnreasoned() = detectionRequirements.values
        .filter { it.isPrimary() }
        .none { it.isRequired() }

    fun reasonsFor(requirementName: RequirementName): List<Reason> {
        val requirement = detectionRequirements[requirementName]!!
        return requirement.reasons()
    }

    fun summary(): String {
        return detectionRequirements.values
            .joinToString { "${it.name().value()}: ${it.numberOfReasons()}" }
    }

    companion object {
        @JvmStatic
        fun empty(
            primaryRequirements: List<RequirementName>,
            secondaryRequirements: List<RequirementName>
        ): DetectionRequirements {
            val requirementMap = LinkedHashMap<RequirementName, DetectionRequirement>()
            primaryRequirements.forEach { requirementMap[it] = primaryRequirement(it) }
            secondaryRequirements.forEach { requirementMap[it] = secondaryRequirement(it) }
            return DetectionRequirements(requirementMap)
        }
    }
}
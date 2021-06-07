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
package de.quantummaid.reflectmaid.graalvm

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.ReflectionExecutorFactory
import de.quantummaid.reflectmaid.TypeToken
import de.quantummaid.reflectmaid.resolvedtype.ClassType

fun ReflectMaid.registerToGraalVm(
    registerReflections: Boolean = true,
    registerDynamicProxies: Boolean = true,
    registry: ReflectMaidRegistry = REFLECTMAID_REGISTRY
) {
    val registeredReflectMaid = RegisteredReflectMaid(this, registerDynamicProxies, registerReflections)
    registry.addReflectMaid(registeredReflectMaid)
}

data class RegisteredReflectMaid(
    val reflectMaid: ReflectMaid,
    val registerDynamicProxies: Boolean,
    val registerReflections: Boolean
)

val REFLECTMAID_REGISTRY = ReflectMaidRegistry()

class ReflectMaidRegistry {
    private val reflectMaids = mutableListOf<RegisteredReflectMaid>()

    fun addReflectMaid(registeredReflectMaid: RegisteredReflectMaid) {
        reflectMaids.add(registeredReflectMaid)
    }

    fun registerReflections(registerer: (List<ClassType>) -> Unit) {
        val reflections = reflectMaids
            .filter { it.registerReflections }
            .map { it.reflectMaid }
            .flatMap { it.registeredTypes() }
            .filterIsInstance<ClassType>()
            .filter { it.assignableType() != TypeToken::class.java }
            .distinct()
        if (reflections.isNotEmpty()) {
            registerer.invoke(reflections)
        }
    }

    fun registerDynamicProxies(registerer: (List<Class<*>>) -> Unit) {
        val dynamicProxies = reflectMaids
            .filter { it.registerDynamicProxies }
            .map { it.reflectMaid }
            .map { it.executorFactory }
            .filterIsInstance<ReflectionExecutorFactory>()
            .flatMap { it.registeredDynamicProxies() }
            .map { it.assignableType() }
            .distinct()
        if (dynamicProxies.isNotEmpty()) {
            registerer.invoke(dynamicProxies)
        }
    }
}
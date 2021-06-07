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

import com.oracle.svm.core.annotate.AutomaticFeature
import com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.ReflectionExecutorFactory
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport

data class RegisteredReflectMaid(
    val reflectMaid: ReflectMaid,
    val registerDynamicProxies: Boolean,
    val registerReflections: Boolean
)

@AutomaticFeature
class ReflectMaidFeature : Feature {

    companion object {
        private val registeredReflectMaids = mutableListOf<RegisteredReflectMaid>()

        @JvmStatic
        fun addReflectMaid(registeredReflectMaid: RegisteredReflectMaid) {
            registeredReflectMaids.add(registeredReflectMaid)
        }
    }

    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
        registerReflections()
        registerDynamicProxies()
    }

    override fun afterAnalysis(access: Feature.AfterAnalysisAccess?) {
    }

    private fun registerReflections() {
        val reflections = registeredReflectMaids
            .filter { it.registerReflections }
            .map { it.reflectMaid }
            .flatMap { it.registeredTypes() }
            .distinct()
        if (reflections.isNotEmpty()) {
            val runtimeReflectionSupport = ImageSingletons.lookup(RuntimeReflectionSupport::class.java)
            reflections.forEach { registerReflections(it, runtimeReflectionSupport) }
        }
    }

    private fun registerReflections(resolvedType: ResolvedType, runtimeReflectionSupport: RuntimeReflectionSupport) {
        RuntimeReflection.register(resolvedType.assignableType())
        resolvedType.methods()
            .map { it.method }
            .forEach { runtimeReflectionSupport.register(it) }
        resolvedType.constructors()
            .map { it.constructor }
            .forEach { runtimeReflectionSupport.register(it) }
        resolvedType.fields()
            .map { it.field }
            .forEach { runtimeReflectionSupport.register(false, false, it) }
    }

    private fun registerDynamicProxies() {
        val dynamicProxies = registeredReflectMaids
            .filter { it.registerDynamicProxies }
            .map { it.reflectMaid }
            .map { it.executorFactory }
            .filterIsInstance<ReflectionExecutorFactory>()
            .flatMap { it.registeredDynamicProxies() }
            .map { it.assignableType() }
            .distinct()
        if (dynamicProxies.isNotEmpty()) {
            val dynamicProxyRegistry = ImageSingletons.lookup(DynamicProxyRegistry::class.java)
            dynamicProxies.forEach { dynamicProxyRegistry.addProxyClass(it) }
        }
    }
}
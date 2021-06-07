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
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import org.graalvm.nativeimage.impl.RuntimeReflectionSupport

@AutomaticFeature
class ReflectMaidFeature : Feature {
    private var registered = false

    override fun duringAnalysis(access: Feature.DuringAnalysisAccess) {
        if (registered) {
            return
        }
        REFLECTMAID_REGISTRY.registerReflections { reflections ->
            val runtimeReflectionSupport = ImageSingletons.lookup(RuntimeReflectionSupport::class.java)
            reflections.forEach { registerReflections(it, runtimeReflectionSupport) }
            access.requireAnalysisIteration()
        }
        REFLECTMAID_REGISTRY.registerDynamicProxies { dynamicProxies ->
            val dynamicProxyRegistry = ImageSingletons.lookup(DynamicProxyRegistry::class.java)
            dynamicProxies.forEach { dynamicProxyRegistry.addProxyClass(it) }
        }
        registered = true
    }

    private fun registerReflections(type: ClassType, runtimeReflectionSupport: RuntimeReflectionSupport) {
        RuntimeReflection.register(type.assignableType())
        type.cachedMethods()
            .map { it.method }
            .forEach { runtimeReflectionSupport.register(it) }
        type.cachedConstructors()
            .map { it.constructor }
            .forEach { runtimeReflectionSupport.register(it) }
        type.cachedFields()
            .map { it.field }
            .forEach { runtimeReflectionSupport.register(false, false, it) }
    }
}
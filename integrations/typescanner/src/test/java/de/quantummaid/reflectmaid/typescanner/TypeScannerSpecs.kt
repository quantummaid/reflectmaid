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
package de.quantummaid.reflectmaid.typescanner

import de.quantummaid.reflectmaid.typescanner.Processor.Companion.processor
import de.quantummaid.reflectmaid.typescanner.Reason.Companion.becauseOf
import de.quantummaid.reflectmaid.typescanner.Reason.Companion.manuallyAdded
import de.quantummaid.reflectmaid.typescanner.TypeIdentifier.Companion.uniqueVirtualTypeIdentifier
import de.quantummaid.reflectmaid.typescanner.factories.StateFactories
import de.quantummaid.reflectmaid.typescanner.factories.StateFactory
import de.quantummaid.reflectmaid.typescanner.factories.UndetectedFactory
import de.quantummaid.reflectmaid.typescanner.log.StateLog
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementName
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.scopes.Scope.Companion.rootScope
import de.quantummaid.reflectmaid.typescanner.signals.AddReasonSignal.Companion.addReasonSignal
import de.quantummaid.reflectmaid.typescanner.signals.Signal
import de.quantummaid.reflectmaid.typescanner.states.DetectionResult
import de.quantummaid.reflectmaid.typescanner.states.Detector
import de.quantummaid.reflectmaid.typescanner.states.RequirementsDescriber
import de.quantummaid.reflectmaid.typescanner.states.Resolver
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class MyDefinition(val value: String, val dependencies: List<TypeIdentifier> = emptyList())

class MyStateFactory(val typeIdentifier: TypeIdentifier, val definition: MyDefinition) : StateFactory<MyDefinition> {

    override fun applies(type: TypeIdentifier) = typeIdentifier == type

    override fun create(type: TypeIdentifier, context: Context<MyDefinition>) {
        context.setManuallyConfiguredResult(definition)
    }
}

class MyDetector(val definitions: Map<TypeIdentifier, MyDefinition>) : Detector<MyDefinition> {
    override fun detect(
        type: TypeIdentifier,
        scope: Scope,
        detectionRequirements: DetectionRequirements
    ): DetectionResult<MyDefinition> {
        return DetectionResult.success(definitions[type]!!)
    }
}

class MyResolver : Resolver<MyDefinition> {
    override fun resolve(
        result: MyDefinition,
        type: TypeIdentifier,
        scope: Scope,
        detectionRequirements: DetectionRequirements
    ): List<Signal<MyDefinition>> {
        println(scope.render())
        return result.dependencies.map { addReasonSignal(it, scope, REGISTERED, becauseOf(type, scope)) }
    }
}

class MyOnCollectionError : OnCollectionError<MyDefinition> {
    override fun onCollectionError(
        results: Map<TypeIdentifier, Map<Scope, CollectionResult<MyDefinition>>>,
        log: StateLog<MyDefinition>,
        failures: Map<TypeIdentifier, Map<Scope, Report<MyDefinition>>>
    ) {
        throw UnsupportedOperationException()
    }
}

class MyRequirementsDescriber : RequirementsDescriber {
    override fun describe(detectionRequirements: DetectionRequirements): String {
        throw UnsupportedOperationException()
    }
}

val REGISTERED = RequirementName("registered")

class TypeScannerSpecs {

    @Test
    fun typeScannerCanHaveScopes() {
        val typeIdentifier = uniqueVirtualTypeIdentifier()
        val stateFactories = StateFactories<MyDefinition>(
            emptyMap(),
            UndetectedFactory()
        )
        val detector = MyDetector(
            mapOf(
                typeIdentifier to MyDefinition("foo")
            )
        )
        val processor = processor(
            stateFactories,
            listOf(REGISTERED),
            emptyList()
        )

        val scope = rootScope().childScope(uniqueVirtualTypeIdentifier())
        processor.dispatch(addReasonSignal(typeIdentifier, scope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(detector, MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

        val byScope = resultMap[typeIdentifier]
        assertThat(byScope, `is`(notNullValue()))
        assertThat(byScope!!.size, `is`(1))

        val scopedResult = byScope[scope]
        assertThat(scopedResult, `is`(notNullValue()))
        assertThat(scopedResult!!.definition.value, `is`("foo"))
    }

    @Test
    fun dependenciesHaveSameScope() {
        val typeIdentifier = uniqueVirtualTypeIdentifier()
        val dependency = uniqueVirtualTypeIdentifier()

        val stateFactories = StateFactories<MyDefinition>(
            emptyMap(),
            UndetectedFactory()
        )
        val detector = MyDetector(
            mapOf(
                typeIdentifier to MyDefinition("foo", listOf(dependency)),
                dependency to MyDefinition("bar")
            )
        )
        val processor = processor(
            stateFactories,
            listOf(REGISTERED),
            emptyList()
        )

        val scopeIdentifier = uniqueVirtualTypeIdentifier()
        val scope = rootScope().childScope(scopeIdentifier)
        processor.dispatch(addReasonSignal(typeIdentifier, scope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(detector, MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

        val byScope = resultMap[dependency]
        assertThat(byScope, `is`(notNullValue()))
        assertThat(byScope!!.size, `is`(1))

        val scopedResult = byScope[scope]
        assertThat(scopedResult, `is`(notNullValue()))
        assertThat(scopedResult!!.definition.value, `is`("bar"))
    }

    @Test
    fun factoriesInBetterScopeAreUsed() {
        val typeIdentifier = uniqueVirtualTypeIdentifier()

        val rootScope = rootScope()
        val childScope = rootScope.childScope(uniqueVirtualTypeIdentifier())
        val stateFactories = StateFactories(
            mapOf(
                rootScope() to listOf(
                    MyStateFactory(typeIdentifier, MyDefinition("foo"))
                ),
                childScope to listOf(
                    MyStateFactory(typeIdentifier, MyDefinition("bar"))
                )
            ),
            UndetectedFactory()
        )
        val processor = processor(
            stateFactories,
            listOf(REGISTERED),
            emptyList()
        )

        processor.dispatch(addReasonSignal(typeIdentifier, rootScope, REGISTERED, manuallyAdded()))
        processor.dispatch(addReasonSignal(typeIdentifier, childScope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(MyDetector(emptyMap()), MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

        val byScope = resultMap[typeIdentifier]

        assertThat(byScope, `is`(notNullValue()))
        assertThat(byScope!!.size, `is`(2))

        val rootScopedResult = byScope[rootScope]
        assertThat(rootScopedResult, `is`(notNullValue()))
        assertThat(rootScopedResult!!.definition.value, `is`("foo"))

        val childScopedResult = byScope[childScope]
        assertThat(childScopedResult, `is`(notNullValue()))
        assertThat(childScopedResult!!.definition.value, `is`("bar"))
    }

    @Test
    fun definitionsGetTheScopeOfTheirFactory() {
        val typeIdentifier = uniqueVirtualTypeIdentifier()
        val dependency = uniqueVirtualTypeIdentifier()

        val rootScope = rootScope()
        val childScope = rootScope.childScope(uniqueVirtualTypeIdentifier())
        val childChildScope = childScope.childScope(uniqueVirtualTypeIdentifier())
        val stateFactories = StateFactories(
            mapOf(
                rootScope() to listOf(
                    MyStateFactory(dependency, MyDefinition("foo"))
                ),
                childScope to listOf(
                    MyStateFactory(dependency, MyDefinition("bar"))
                ),
                childChildScope to listOf(
                    MyStateFactory(
                        typeIdentifier, MyDefinition("asdf", listOf(dependency))
                    )
                )
            ),
            UndetectedFactory()
        )
        val processor = processor(
            stateFactories,
            listOf(REGISTERED),
            emptyList()
        )

        processor.dispatch(addReasonSignal(typeIdentifier, childChildScope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(MyDetector(emptyMap()), MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

        val byScope = resultMap[dependency]

        assertThat(byScope, `is`(notNullValue()))
        assertThat(byScope!!.size, `is`(1))

        val childScopedResult = byScope[childScope]
        assertThat(childScopedResult, `is`(notNullValue()))
        assertThat(childScopedResult!!.definition.value, `is`("bar"))
    }
}
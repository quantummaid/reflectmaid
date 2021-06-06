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
import de.quantummaid.reflectmaid.typescanner.factories.StateFactory
import de.quantummaid.reflectmaid.typescanner.log.StateLog
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementName
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.scopes.Scope.Companion.rootScope
import de.quantummaid.reflectmaid.typescanner.signals.AddReasonSignal.Companion.addReasonSignal
import de.quantummaid.reflectmaid.typescanner.signals.Signal
import de.quantummaid.reflectmaid.typescanner.states.*
import de.quantummaid.reflectmaid.typescanner.states.detected.Unreasoned
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class MyDefinition(val value: String, val dependencies: List<TypeIdentifier> = emptyList())

class MyStateFactory(val typeIdentifier: TypeIdentifier, val definition: MyDefinition) : StateFactory<MyDefinition> {
    override fun create(type: TypeIdentifier, context: Context<MyDefinition>): StatefulDefinition<MyDefinition>? {
        return if (typeIdentifier == type) {
            context.setManuallyConfiguredResult(definition)
            Unreasoned(context)
        } else {
            null
        }
    }
}

class MyDetector : Detector<MyDefinition> {
    override fun detect(
        type: TypeIdentifier,
        scope: Scope,
        detectionRequirements: DetectionRequirements
    ): DetectionResult<MyDefinition> {
        throw UnsupportedOperationException()
    }
}

class MyResolver : Resolver<MyDefinition> {
    override fun resolve(
        result: MyDefinition,
        type: TypeIdentifier,
        scope: Scope,
        detectionRequirements: DetectionRequirements
    ): List<Signal<MyDefinition>> {
        return result.dependencies.map { addReasonSignal(it, scope, REGISTERED, becauseOf(type)) }
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
        val processor = processor(
            listOf(MyStateFactory(typeIdentifier, MyDefinition("foo"))),
            listOf(REGISTERED),
            emptyList()
        )

        val scope = rootScope().childScope(uniqueVirtualTypeIdentifier())
        processor.dispatch(addReasonSignal(typeIdentifier, scope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(MyDetector(), MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

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
        val processor = processor(
            listOf(
                MyStateFactory(typeIdentifier, MyDefinition("foo", listOf(dependency))),
                MyStateFactory(dependency, MyDefinition("bar"))
            ),
            listOf(REGISTERED),
            emptyList()
        )

        val scopeIdentifier = uniqueVirtualTypeIdentifier()
        val scope = rootScope().childScope(scopeIdentifier)
        processor.dispatch(addReasonSignal(typeIdentifier, scope, REGISTERED, manuallyAdded()))
        val resultMap = processor.collect(MyDetector(), MyResolver(), MyOnCollectionError(), MyRequirementsDescriber())

        val byScope = resultMap[dependency]
        assertThat(byScope, `is`(notNullValue()))
        assertThat(byScope!!.size, `is`(1))

        val scopedResult = byScope[scope]
        assertThat(scopedResult, `is`(notNullValue()))
        assertThat(scopedResult!!.definition.value, `is`("bar"))
    }
}
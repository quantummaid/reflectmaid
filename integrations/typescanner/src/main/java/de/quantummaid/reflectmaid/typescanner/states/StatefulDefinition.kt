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
package de.quantummaid.reflectmaid.typescanner.states

import de.quantummaid.reflectmaid.typescanner.Context
import de.quantummaid.reflectmaid.typescanner.Report
import de.quantummaid.reflectmaid.typescanner.TypeIdentifier
import de.quantummaid.reflectmaid.typescanner.requirements.DetectionRequirements
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementsReducer
import de.quantummaid.reflectmaid.typescanner.scopes.Scope
import de.quantummaid.reflectmaid.typescanner.signals.Signal

fun interface Resolver<T> {
    fun resolve(result: T,
                type: TypeIdentifier,
                scope: Scope,
                detectionRequirements: DetectionRequirements): List<Signal<T>>
}

interface RequirementsDescriber {
    fun describe(detectionRequirements: DetectionRequirements): String
}

interface Detector<T> {
    fun detect(type: TypeIdentifier, scope: Scope, detectionRequirements: DetectionRequirements): DetectionResult<T>
}

abstract class StatefulDefinition<T>(val context: Context<T>) {

    abstract fun changeRequirements(reducer: RequirementsReducer): StatefulDefinition<T>

    fun type(): TypeIdentifier {
        return context.type
    }

    fun scope(): Scope {
        return context.scope
    }

    open fun detect(detector: Detector<T>, requirementsDescriber: RequirementsDescriber): StatefulDefinition<T> {
        return this
    }

    open fun resolve(resolver: Resolver<T>): StatefulDefinition<T> {
        return this
    }

    open fun getDefinition(requirementsDescriber: RequirementsDescriber): Report<T> {
        throw UnsupportedOperationException(this.javaClass.toString() + " " + context.toString())
    }

    open fun matches(otherType: TypeIdentifier, otherScope: Scope): Boolean {
        if (context.type != otherType) {
            return false
        }
        return context.scope.contains(otherScope)
    }

    override fun equals(other: Any?): Boolean {
        other as StatefulDefinition<T>
        return other.type() == type()
    }

    override fun hashCode(): Int {
        return type().hashCode()
    }
}
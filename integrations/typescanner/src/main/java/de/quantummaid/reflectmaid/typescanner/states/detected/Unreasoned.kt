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
package de.quantummaid.reflectmaid.typescanner.states.detected

import de.quantummaid.reflectmaid.typescanner.Context
import de.quantummaid.reflectmaid.typescanner.Report
import de.quantummaid.reflectmaid.typescanner.Report.Companion.empty
import de.quantummaid.reflectmaid.typescanner.requirements.RequirementsReducer
import de.quantummaid.reflectmaid.typescanner.states.RequirementsDescriber
import de.quantummaid.reflectmaid.typescanner.states.StatefulDefinition

class Unreasoned<T> constructor(context: Context<T>) : StatefulDefinition<T>(context) {
    override fun changeRequirements(reducer: RequirementsReducer): StatefulDefinition<T> {
        val requiredAction = context.changeRequirements(reducer)
        return requiredAction.map(
            { this },
            { ToBeDetected(context) }
        ) { Unreasoned(context) }
    }

    override fun getDefinition(requirementsDescriber: RequirementsDescriber): Report<T> {
        return empty()
    }
}
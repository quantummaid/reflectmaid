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
package de.quantummaid.reflectmaid.resolvedtype.resolver

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.ReflectMaid
import java.util.Arrays
import java.util.stream.Collectors
import de.quantummaid.reflectmaid.validators.NotNullValidator
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import java.lang.reflect.Executable
import java.lang.reflect.Parameter

data class ResolvedParameter(val type: ResolvedType,
                             val parameter: Parameter) {

    fun name(): String {
        return parameter.name
    }

    companion object {
        @JvmStatic
        fun resolveParameters(reflectMaid: ReflectMaid,
                              executable: Executable,
                              fullType: ClassType): List<ResolvedParameter> {
            return Arrays.stream(executable.parameters)
                    .map { parameter: Parameter -> resolveParameter(reflectMaid, fullType, parameter) }
                    .collect(Collectors.toList())
        }

        private fun resolveParameter(reflectMaid: ReflectMaid,
                                     declaringType: ClassType,
                                     parameter: Parameter): ResolvedParameter {
            NotNullValidator.validateNotNull(declaringType, "declaringType")
            NotNullValidator.validateNotNull(parameter, "parameter")
            val parameterizedType = parameter.parameterizedType
            val resolvedType = reflectMaid.resolve(fromReflectionType<Any>(parameterizedType, declaringType))
            return ResolvedParameter(resolvedType, parameter)
        }
    }
}
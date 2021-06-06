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
package de.reflectmaid.quantummaid.javapoet

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType

fun ResolvedType.toTypeName(): TypeName {
    val assignableType = assignableType()
    val className = TypeName.get(assignableType)
    if (className !is ClassName) {
        return className
    }
    val typeParameters = typeParameters()
    if (typeParameters.isEmpty()) {
        return className
    }
    val typeVariables = typeParameters
            .map { it.toTypeName() }
    return ParameterizedTypeName.get(className, *typeVariables.toTypedArray())
}

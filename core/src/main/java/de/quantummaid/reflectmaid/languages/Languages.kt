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
package de.quantummaid.reflectmaid.languages

data class ParameterData(val name: String, val type: String)

interface Language {

    companion object {
        val KOTLIN = Kotlin()
        val JAVA = Java()
    }

    fun wildcard(): String

    fun array(componentType: String): String

    fun method(name: String, parameters: List<ParameterData>, returnType: String?): String
}

class Kotlin : Language {

    override fun wildcard() = "*"

    override fun array(componentType: String) = "Array<$componentType>"

    override fun method(name: String, parameters: List<ParameterData>, returnType: String?): String {
        val parametersString = parameters.joinToString { "${it.name}: ${it.type}" }
        val returnTypeDescription = returnType?.let { ": $it" } ?: ""
        return "fun ${name}($parametersString)$returnTypeDescription"
    }
}

class Java : Language {

    override fun wildcard() = "?"

    override fun array(componentType: String) = "$componentType[]"

    override fun method(name: String, parameters: List<ParameterData>, returnType: String?): String {
        val parametersString = parameters.joinToString { "${it.type} ${it.name}" }
        val returnTypeDescription = returnType ?: "void"
        return "$returnTypeDescription ${name}($parametersString)"
    }
}
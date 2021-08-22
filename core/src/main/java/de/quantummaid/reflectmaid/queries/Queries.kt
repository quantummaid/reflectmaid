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
package de.quantummaid.reflectmaid.queries

import de.quantummaid.reflectmaid.GenericType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.queries.QueryNotFoundException.Companion.queryNotFoundBecauseNoFollowUpException
import de.quantummaid.reflectmaid.queries.QueryNotFoundException.Companion.queryNotFoundException
import de.quantummaid.reflectmaid.queries.QueryPath.Companion.field
import de.quantummaid.reflectmaid.queries.QueryPath.Companion.method
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

sealed class QueryPathElement<FollowUpType, ResultType : QueryResult<FollowUpType>> {
    abstract fun execute(resolvedType: ResolvedType, reflectMaid: ReflectMaid, previous: QueryResult<*>?): ResultType?

    abstract fun render(reflectMaid: ReflectMaid): String
}

class FieldQueryPathElement<FieldType>(val name: String, val type: GenericType<FieldType>?) :
    QueryPathElement<FieldType, QueriedField<FieldType>>() {

    override fun execute(
        resolvedType: ResolvedType,
        reflectMaid: ReflectMaid,
        previous: QueryResult<*>?
    ): QueriedField<FieldType>? {
        val field = (resolvedType.allSupertypes() + resolvedType)
            .flatMap { it.fields() }
            .filter {
                it.isPublic() || it.kotlinGetAccessor() != null
            }
            .find { it.name == name }
            ?: return null
        if (type != null) {
            val resolvedType = reflectMaid.resolve(type)
            if (field.type != resolvedType) {
                return null
            }
        }
        return QueriedField(field, previous)
    }

    override fun render(reflectMaid: ReflectMaid): String {
        val typeString = type
            ?.let { reflectMaid.resolve(it) }
            ?.simpleDescription()
            ?: "*"
        return "$name:$typeString"
    }
}

class MethodQueryPathElement<ReturnType>(
    val name: String,
    val returnType: GenericType<ReturnType>?,
    val parameters: List<GenericType<*>>?
) : QueryPathElement<ReturnType, QueriedMethod<ReturnType>>() {
    override fun execute(
        resolvedType: ResolvedType,
        reflectMaid: ReflectMaid,
        previous: QueryResult<*>?
    ): QueriedMethod<ReturnType>? {
        val byName = (resolvedType.allSupertypes() + resolvedType)
            .flatMap { it.methods() }
            .filter { !it.isAbstract() }
            .filter { it.isPublic() }
            .filter { it.name == name }
        val filteredByReturnType = if (returnType != null) {
            val resolvedReturnType = reflectMaid.resolve(returnType)
            if (resolvedReturnType.assignableType() == Nothing::class.java) {
                byName.filter { it.returnType == null }
            } else {
                byName.filter {
                    it.returnType == resolvedReturnType
                            || (it.returnType != null && resolvedReturnType in it.returnType.allSupertypes())
                }
            }
        } else {
            byName
        }

        val filteredByParameters = if (parameters != null) {
            val resolvedParameters = parameters.map { reflectMaid.resolve(it) }
            filteredByReturnType.filter { it.hasParameters(resolvedParameters) }
        } else {
            filteredByReturnType
        }

        return filteredByParameters
            .singleOrNull()
            ?.let { QueriedMethod(it, previous) }
    }

    override fun render(reflectMaid: ReflectMaid): String {
        val parametersString = parameters
            ?.map { reflectMaid.resolve(it) }
            ?.joinToString(separator = ",") { it.simpleDescription() }
            ?: "*"
        val returnTypeString = returnType
            ?.let { reflectMaid.resolve(it) }
            ?.simpleDescription()
            ?: "*"
        return "$name($parametersString):$returnTypeString"
    }
}

class QueryPath<T>(val elements: List<QueryPathElement<Any, QueryResult<Any>>>) {

    companion object {
        fun field(name: String) = queryPath().field(name)
        fun <FieldType : Any> field(name: String, type: KClass<FieldType>) = queryPath().field(name, type)
        fun <FieldType> field(name: String, type: GenericType<FieldType>) = queryPath().field(name, type)
        fun method(name: String) = queryPath().method(name)
        fun <ReturnType : Any> method(name: String, returnType: KClass<ReturnType>) =
            queryPath().method(name, returnType)

        fun method(name: String, parameters: List<GenericType<*>>): QueryPath<QueriedMethod<Any>> =
            queryPath().method(name, parameters)

        fun <ReturnType> method(name: String, returnType: GenericType<ReturnType>) =
            queryPath().method(name, returnType)

        fun <ReturnType> method(name: String, returnType: GenericType<ReturnType>, parameters: List<GenericType<*>>) =
            queryPath().method(name, returnType, parameters)

        private fun queryPath() = QueryPath<Any>(emptyList())
    }

    fun field(name: String): QueryPath<QueriedField<Any>> {
        val element = FieldQueryPathElement<Any>(name, null)
        return extend(element)
    }

    fun <FieldType : Any> field(name: String, type: KClass<FieldType>): QueryPath<QueriedField<FieldType>> {
        val genericType = genericType(type)
        val element = FieldQueryPathElement(name, genericType)
        return extend(element)
    }

    fun <FieldType> field(name: String, type: GenericType<FieldType>): QueryPath<QueriedField<FieldType>> {
        val element = FieldQueryPathElement(name, type)
        return extend(element)
    }

    fun method(name: String): QueryPath<QueriedMethod<Any>> {
        val element = MethodQueryPathElement<Any>(name, null, null)
        return extend(element)
    }

    fun <ReturnType : Any> method(name: String, returnType: KClass<ReturnType>): QueryPath<QueriedMethod<ReturnType>> {
        val genericType = genericType(returnType)
        return method(name, genericType)
    }

    fun <ReturnType> method(name: String, returnType: GenericType<ReturnType>): QueryPath<QueriedMethod<ReturnType>> {
        val element = MethodQueryPathElement(name, returnType, null)
        return extend(element)
    }

    fun method(name: String, parameters: List<GenericType<*>>): QueryPath<QueriedMethod<Any>> {
        val element = MethodQueryPathElement<Any>(name, null, parameters)
        return extend(element)
    }

    fun <ReturnType> method(
        name: String,
        returnType: GenericType<ReturnType>,
        parameters: List<GenericType<*>>
    ): QueryPath<QueriedMethod<ReturnType>> {
        val element = MethodQueryPathElement(name, returnType, parameters)
        return extend(element)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <FollowUpType, ResultType : QueryResult<FollowUpType>> extend(element: QueryPathElement<FollowUpType, ResultType>): QueryPath<ResultType> {
        element as QueryPathElement<Any, QueryResult<Any>>
        return QueryPath(elements + element)
    }

    @Suppress("UNCHECKED_CAST")
    internal fun extract(resolvedType: ResolvedType, reason: String?, reflectMaid: ReflectMaid): T {
        var currentResult: QueryResult<Any>? = null
        var currentType: ResolvedType? = resolvedType
        val matched = mutableListOf<QueryResult<*>>()
        elements.forEach {
            if (currentType == null) {
                throw queryNotFoundBecauseNoFollowUpException(this, it, matched, currentResult!!, reflectMaid, reason)
            }
            currentResult = it.execute(currentType!!, reflectMaid, currentResult)
            if (currentResult == null) {
                throw queryNotFoundException(this, it, matched, resolvedType, reflectMaid, reason)
            }
            matched.add(currentResult!!)
            currentType = followUpType(currentResult!!)
        }
        return currentResult as T
    }

    private fun followUpType(queryResult: QueryResult<Any>): ResolvedType? {
        return when (queryResult) {
            is QueriedField<*> -> queryResult.resolvedField.type
            is QueriedMethod<*> -> {
                if (canHaveFollowUp(queryResult.resolvedMethod)) {
                    queryResult.resolvedMethod.returnType
                } else {
                    null
                }
            }
        }
    }
}

fun canHaveFollowUp(method: ResolvedMethod): Boolean {
    return method.parameters.isEmpty() && method.returnType != null
}

@Suppress("UNCHECKED_CAST")
operator fun <FieldType> ResolvedType.get(property: KProperty<FieldType>): QueriedField<FieldType> {
    return query(field(property.name)) as QueriedField<FieldType>
}

@Suppress("UNCHECKED_CAST")
operator fun <ReturnType> ResolvedType.get(function: KFunction<ReturnType>): QueriedMethod<ReturnType> {
    return query(method(function.name)) as QueriedMethod<ReturnType>
}

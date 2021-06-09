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
package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.fromResolvedType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.types.TestType
import de.quantummaid.reflectmaid.types.TypeWithFields
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheSpecs {

    @Test
    fun typeVariableGetsRegisteredInCache() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        reflectMaid.resolve<List<String>>()
        val registeredTypes = reflectMaid.registeredTypes()
        assertThat(registeredTypes.map { it.simpleDescription() }, containsInAnyOrder("String", "List<String>", "TypeToken<List<String>>"))
    }

    @Test
    fun reflectMaidCanResolveDirectlyFromJavaClass() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType1 = reflectMaid.resolve(String::class.java)
        val resolvedType2 = reflectMaid.resolve(genericType(String::class.java))
        assertTrue(resolvedType1 === resolvedType2)
    }

    @Test
    fun reflectMaidCanResolveDirectlyFromKotlinClass() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType1 = reflectMaid.resolve(String::class)
        val resolvedType2 = reflectMaid.resolve(genericType(String::class.java))
        assertTrue(resolvedType1 === resolvedType2)
    }

    @Test
    fun reflectMaidCanResolveDirectlyFromTypeToken() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType1 = reflectMaid.resolve(String::class)
        val resolvedType2 = reflectMaid.resolve<String>()
        assertTrue(resolvedType1 === resolvedType2)
    }

    @Test
    fun reflectMaidCanProvideRegisteredTypes() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val stringResolvedType = reflectMaid.resolve(String::class)
        val anyResolvedType = reflectMaid.resolve(genericType<List<String>>(List::class, genericType(String::class.java)))

        reflectMaid.resolve(String::class.java)

        val registeredTypes = reflectMaid.registeredTypes()

        assertThat(registeredTypes, hasSize(2))
        assertThat(registeredTypes, containsInAnyOrder(stringResolvedType, anyResolvedType))
    }

    @Test
    fun reflectMaidRegistersTypesRecursively() {
        val reflectMaid = ReflectMaid.aReflectMaid()

        val resolvedType = reflectMaid.resolve(TypeWithFields::class)

        assertThat(reflectMaid.registeredTypes(), hasSize(1))

        resolvedType as ClassType
        resolvedType.fields()

        assertThat(reflectMaid.registeredTypes(), hasSize(2))
    }

    @Test
    fun twoWildcardGenericTypesAreEqual() {
        val wildcard1 = wildcard()
        val wildcard2 = wildcard()
        assertEquals(wildcard1, wildcard2)
        assertEquals(wildcard1.hashCode(), wildcard2.hashCode())
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForMixedGenericTypes() {
        val reflectMaid = ReflectMaid.aReflectMaid()

        val genericType1 = genericType<List<String>>(List::class, String::class)
        val resolvedType1 = reflectMaid.resolve(genericType1)

        val genericType2 = genericType<List<String>>()
        val resolvedType2 = reflectMaid.resolve(genericType2)

        assertTrue(resolvedType1 === resolvedType2)
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameJavaClassBasedGenericType() {
        assertSameReferenceGetsReturned { genericType(String::class.java) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameJavaClassWithGenericsBasedGenericType() {
        assertSameReferenceGetsReturned { genericType<List<String>>(List::class.java, String::class.java) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameKotlinClassBasedGenericType() {
        assertSameReferenceGetsReturned { genericType(String::class) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameKotlinClassWithGenericsBasedGenericType() {
        assertSameReferenceGetsReturned { genericType<List<String>>(List::class, String::class) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameReifiedGenericBasedGenericType() {
        assertSameReferenceGetsReturned { genericType<String>() }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameResolvedTypeBasedGenericType() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve(String::class.java)
        assertSameReferenceGetsReturned { fromResolvedType<String>(resolvedType) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameWildcardBasedGenericType() {
        assertSameReferenceGetsReturned { wildcard() }
    }

    @Test
    fun cachedMethodsCanBeQueried() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val classType = reflectMaid.resolve<TestType>() as ClassType
        assertThat(classType.cachedMethods().size, `is`(0))
        classType.methods()
        assertThat(classType.cachedMethods().size, `is`(1))
    }

    @Test
    fun cachedFieldsCanBeQueried() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val classType = reflectMaid.resolve<TestType>() as ClassType
        assertThat(classType.cachedFields().size, `is`(0))
        classType.fields()
        assertThat(classType.cachedFields().size, `is`(1))
    }

    @Test
    fun cachedConstructorsCanBeQueried() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val classType = reflectMaid.resolve<TestType>() as ClassType
        assertThat(classType.cachedConstructors().size, `is`(0))
        classType.constructors()
        assertThat(classType.cachedConstructors().size, `is`(1))
    }

    private fun assertSameReferenceGetsReturned(genericTypeFactory: () -> GenericType<*>) {
        val reflectMaid = ReflectMaid.aReflectMaid()

        val genericType1 = genericTypeFactory.invoke()
        val resolvedType1 = reflectMaid.resolve(genericType1)

        val genericType2 = genericTypeFactory.invoke()
        val resolvedType2 = reflectMaid.resolve(genericType2)

        assertTrue(resolvedType1 === resolvedType2)
    }
}
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

import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import de.quantummaid.reflectmaid.ReflectMaid.Companion.aReflectMaid
import de.quantummaid.reflectmaid.languages.Language.Companion.JAVA
import de.quantummaid.reflectmaid.types.TestTypeWithPrimitiveField
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

open class SuperType
open class SubType : SuperType()
open class GenericSuperType<T>
open class GenericSubType<T> : GenericSuperType<T>()

interface Interface0
interface Interface1
interface Interface2
class ImplementingClass : Interface0, Interface1, Interface2

interface GenericInterface0<T>
interface GenericInterface1<T>
interface GenericInterface2<T>
class GenericImplementingClass<T> : GenericInterface0<T>, GenericInterface1<T>, GenericInterface2<T>

interface ImplementingInterface : Interface0, Interface1, Interface2
interface GenericImplementingInterface<T> : GenericInterface0<T>, GenericInterface1<T>, GenericInterface2<T>

class NestedImplementingSubType : SubType(), ImplementingInterface
class GenericNestedImplementingSubType<T> : GenericSubType<T>(), GenericImplementingInterface<T>

class InheritanceSpecs {

    @Test
    fun superClassesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<SubType>()
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass!!.simpleDescription(), `is`("SuperType"))
    }

    @Test
    fun genericSuperClassesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<GenericSubType<String>>()
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass!!.simpleDescription(), `is`("GenericSuperType<String>"))
    }

    @Test
    fun interfacesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<ImplementingClass>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(
            directInterfaces.map { it.simpleDescription() }, contains(
                "Interface0",
                "Interface1",
                "Interface2"
            )
        )
    }

    @Test
    fun genericInterfacesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<GenericImplementingClass<String>>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(
            directInterfaces.map { it.simpleDescription() }, contains(
                "GenericInterface0<String>",
                "GenericInterface1<String>",
                "GenericInterface2<String>"
            )
        )
    }

    @Test
    fun superTypeOfInterfaceIsNull() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<ImplementingInterface>()
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass, `is`(nullValue()))
    }

    @Test
    fun interfacesOfInterfaceCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<ImplementingInterface>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(
            directInterfaces.map { it.simpleDescription() }, contains(
                "Interface0",
                "Interface1",
                "Interface2"
            )
        )
    }

    @Test
    fun genericInterfacesOfInterfaceCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<GenericImplementingInterface<String>>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(
            directInterfaces.map { it.simpleDescription() }, contains(
                "GenericInterface0<String>",
                "GenericInterface1<String>",
                "GenericInterface2<String>"
            )
        )
    }

    @Test
    fun superClassOfPrimitiveIsNull() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<TestTypeWithPrimitiveField>()
        val intType = resolvedType.fields()[0].type
        val directSuperClass = intType.directSuperClass()
        assertThat(directSuperClass, `is`(nullValue()))
    }

    @Test
    fun interfacesOfPrimitiveAreEmpty() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<TestTypeWithPrimitiveField>()
        val intType = resolvedType.fields()[0].type
        val directInterfaces = intType.directInterfaces()
        assertThat(directInterfaces, empty())
    }

    @Test
    fun superClassOfTypeThatDoesNotExtendIsObject() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<SuperType>()
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass!!.simpleDescription(), `is`("Object"))
    }

    @Test
    fun interfacesOfTypeThatDoesNotImplementAreEmpty() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<SuperType>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(directInterfaces, empty())
    }

    @Test
    fun superClassOfObjectIsNull() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<Any>()
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass, `is`(nullValue()))
    }

    @Test
    fun interfacesOfObjectAreEmpty() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<Any>()
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(directInterfaces, empty())
    }

    @Test
    fun superClassOfArrayIsObject() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<Array<String>>()

        assertThat(resolvedType.simpleDescription(JAVA), `is`("String[]"))

        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass!!.simpleDescription(), `is`("Object"))
    }

    @Test
    fun interfacesOfArrayAreCloneableAndSerializable() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<Array<String>>()

        assertThat(resolvedType.simpleDescription(JAVA), `is`("String[]"))

        val directInterfaces = resolvedType.directInterfaces()
        assertThat(directInterfaces.map { it.simpleDescription() }, containsInAnyOrder("Serializable", "Cloneable"))
    }

    @Test
    fun superClassOfWildcardIsNull() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        val directSuperClass = resolvedType.directSuperClass()
        assertThat(directSuperClass, `is`(nullValue()))
    }

    @Test
    fun interfacesOfWildcardIsAreEmpty() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        val directInterfaces = resolvedType.directInterfaces()
        assertThat(directInterfaces, empty())
    }

    @Test
    fun allDirectSupertypesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<NestedImplementingSubType>()
        val directSupertypes = resolvedType.directSupertypes()
        assertThat(
            directSupertypes.map { it.simpleDescription() }, contains(
                "ImplementingInterface",
                "SubType"
            )
        )
    }

    @Test
    fun allDirectGenericSupertypesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<GenericNestedImplementingSubType<String>>()
        val directSupertypes = resolvedType.directSupertypes()
        assertThat(
            directSupertypes.map { it.simpleDescription() }, contains(
                "GenericImplementingInterface<String>",
                "GenericSubType<String>"
            )
        )
    }

    @Test
    fun allSupertypesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<NestedImplementingSubType>()
        val directSupertypes = resolvedType.allSupertypes()
        assertThat(
            directSupertypes.map { it.simpleDescription() }, contains(
                "ImplementingInterface",
                "SubType",
                "Interface0",
                "Interface1",
                "Interface2",
                "SuperType",
                "Object"
            )
        )
    }

    @Test
    fun allGenericSupertypesCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val resolvedType = reflectMaid.resolve<GenericNestedImplementingSubType<String>>()
        val directSupertypes = resolvedType.allSupertypes()
        assertThat(
            directSupertypes.map { it.simpleDescription() }, contains(
                "GenericImplementingInterface<String>",
                "GenericSubType<String>",
                "GenericInterface0<String>",
                "GenericInterface1<String>",
                "GenericInterface2<String>",
                "GenericSuperType<String>",
                "Object"
            )
        )
    }
}
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

import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import de.quantummaid.reflectmaid.ReflectMaid.Companion.aReflectMaid
import de.quantummaid.reflectmaid.queries.QueryNotFoundException
import de.quantummaid.reflectmaid.queries.QueryPath.Companion.field
import de.quantummaid.reflectmaid.queries.QueryPath.Companion.method
import de.quantummaid.reflectmaid.queries.TypedGetter
import de.quantummaid.reflectmaid.queries.TypedSetter
import de.quantummaid.reflectmaid.queries.get
import de.quantummaid.reflectmaid.util.withException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class Inner(var innerField: String, @JvmField var innerPublicField: String, private val privateField: String) {
    constructor(field: String) : this(field, field, field)

    fun innerMethod() = "foo"

    fun voidMethod() {
        // do nothing
    }

    private fun privateMethod() {
        // do nothing
    }

    fun methodWithParameters(string: String) {
        // do nothing
    }
}

class Outer(val outerField: Inner) {
    fun outerMethod() = outerField

    fun overloadedMethod() {
        // do nothing
    }

    fun overloadedMethod(string: String) {
        // do nothing
    }
}

interface Supertype
class Subtype : Supertype

class ClassWithHierarchicalReturnType {
    fun method(): Subtype {
        throw UnsupportedOperationException()
    }
}

abstract class AbstractType {
    abstract fun abstractMethod()
}

open class OpenClass(val field: String) {
    fun foo() {
        // do nothing
    }
}

class InheritingClass : OpenClass("foo")

class QuerySpecs {

    @Test
    fun directPublicFieldCanBeQueriedBasedOnName() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(field("innerPublicField"))
        assertThat(result.resolvedField.name, `is`("innerPublicField"))

        val inner = Inner("foo")

        val getter = result.createGetter()
        assertThat(getter.get(inner), `is`("foo"))

        val setter = result.createSetter()
        setter.set(inner, "abc")
        assertThat(inner.innerPublicField, `is`("abc"))
    }

    @Test
    fun directKotlinPropertyCanBeQueriedBasedOnName() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(field("innerField"))
        assertThat(result.resolvedField.name, `is`("innerField"))

        val inner = Inner("foo")

        val getter = result.createGetter()
        assertThat(getter.get(inner), `is`("foo"))

        val setter = result.createSetter()
        setter.set(inner, "abc")
        assertThat(inner.innerField, `is`("abc"))
    }

    @Test
    fun directFieldCanBeQueriedBasedOnNameAndType() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(field("innerField", String::class))
        assertThat(result.resolvedField.name, `is`("innerField"))

        val inner = Inner("foo")

        val getter: TypedGetter<String> = result.createGetter()
        assertThat(getter.get(inner), `is`("foo"))

        val setter: TypedSetter<String> = result.createSetter()
        setter.set(inner, "abc")
        assertThat(inner.innerField, `is`("abc"))
    }

    @Test
    fun nestedFieldCanBeQueriedBasedOnName() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()

        val result = type.query(field("outerField").field("innerField"))

        assertThat(result.resolvedField.name, `is`("innerField"))

        val outer = Outer(Inner("foo"))

        val getter = result.createGetter()
        assertThat(getter.get(outer), `is`("foo"))

        val setter = result.createSetter()
        setter.set(outer, "abc")
        assertThat(outer.outerField.innerField, `is`("abc"))
    }

    @Test
    fun nestedFieldCanBeQueriedBasedOnNameAndType() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val result = type.query(field("outerField", Inner::class).field("innerField", String::class))
        assertThat(result.resolvedField.name, `is`("innerField"))

        val outer = Outer(Inner("foo"))

        val getter: TypedGetter<String> = result.createGetter()
        assertThat(getter.get(outer), `is`("foo"))

        val setter: TypedSetter<String> = result.createSetter()
        setter.set(outer, "abc")
        assertThat(outer.outerField.innerField, `is`("abc"))
    }

    @Test
    fun directMethodCanBeQueriedBasedOnName() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("innerMethod"))
        assertThat(result.resolvedMethod.name, `is`("innerMethod"))

        val inner = Inner("foo")

        val getter = result.createGetter()
        assertThat(getter.get(inner), `is`("foo"))
    }

    @Test
    fun directMethodCanBeQueriedBasedOnNameAndVoid() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("voidMethod", Nothing::class))
        assertThat(result.resolvedMethod.name, `is`("voidMethod"))
    }

    @Test
    fun directMethodCanBeQueriedBasedOnNameAndReturnType() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("innerMethod", String::class))
        assertThat(result.resolvedMethod.name, `is`("innerMethod"))

        val inner = Inner("foo")

        val getter = result.createGetter()
        assertThat(getter.get(inner), `is`("foo"))
    }

    @Test
    fun directMethodCanBeQueriedBasedOnNameAndParameters() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("methodWithParameters", listOf(genericType<String>())))
        assertThat(result.resolvedMethod.name, `is`("methodWithParameters"))
    }

    @Test
    fun nestedMethodCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val result = type.query(method("outerMethod").method("innerMethod"))
        assertThat(result.resolvedMethod.name, `is`("innerMethod"))
    }

    @Test
    fun nestedMixedMethodAndFieldCanBeQueried() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val result = type.query(field("outerField").method("innerMethod"))
        assertThat(result.resolvedMethod.name, `is`("innerMethod"))
    }

    @Test
    fun fieldCanBeQueriedByReference() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type[Inner::innerField]
        assertThat(result.resolvedField.name, `is`("innerField"))
    }

    @Test
    fun methodCanBeQueriedByReference() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type[Inner::innerMethod]
        assertThat(result.resolvedMethod.name, `is`("innerMethod"))
    }

    @Test
    fun returnTypeOfMethodCanBeSpecifiedAsSupertype() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<ClassWithHierarchicalReturnType>()
        type.query(method("method", Supertype::class))
    }

    @Test
    fun fieldNotFoundGivesErrorMessage() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val exception = withException<QueryNotFoundException> { type.query(field("unknown")) }
        assertThat(
            exception.message,
            containsString("unable to find query [unknown:*] in type de.quantummaid.reflectmaid.Outer")
        )
    }

    @Test
    fun methodNotFoundGivesErrorMessage() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val exception = withException<QueryNotFoundException> { type.query(method("unknown")) }
        assertThat(
            exception.message,
            containsString("unable to find query [unknown(*):*] in type de.quantummaid.reflectmaid.Outer")
        )
    }

    @Test
    fun cannotTraverseMethodWithParameters() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val exception =
            withException<QueryNotFoundException> { type.query(method("methodWithParameters").field("abc")) }
        assertThat(
            exception.message,
            containsString("unable to continue matching query [abc:*] from 'fun methodWithParameters(string: String)'")
        )
    }

    @Test
    fun cannotTraverseVoidMethod() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val exception = withException<QueryNotFoundException> { type.query(method("voidMethod").field("abc")) }
        assertThat(
            exception.message,
            containsString("unable to continue matching query [abc:*] from 'fun voidMethod()'")
        )
    }

    @Test
    fun reasonCanBeSpecified() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val exception = withException<QueryNotFoundException> { type.query(method("voidMethod").field("abc"), "foo") }
        assertThat(
            exception.message,
            containsString("reason: foo")
        )
    }

    @Test
    fun arrayCannotBeQueried() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Array<String>>()
        val exception = withException<UnsupportedOperationException> { type.query(method("voidMethod")) }
        assertThat(
            exception.message,
            containsString("can only run queries on classes but tried to run query on java.lang.String[]")
        )
    }

    @Test
    fun wildcardsCannotBeQueried() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve(wildcard())
        val exception = withException<UnsupportedOperationException> { type.query(method("voidMethod")) }
        assertThat(
            exception.message,
            containsString("can only run queries on classes but tried to run query on ?")
        )
    }

    @Test
    fun queryNotFoundIfMultipleMethodsMatch() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val exception = withException<QueryNotFoundException> { type.query(method("overloadedMethod")) }
        assertThat(
            exception.message,
            containsString("unable to find query [overloadedMethod(*):*] in type de.quantummaid.reflectmaid.Outer")
        )
    }

    @Test
    fun abstractMethodsAreIgnored() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<AbstractType>()
        val exception = withException<QueryNotFoundException> { type.query(method("abstractMethod")) }
        assertThat(
            exception.message,
            containsString("unable to find query [abstractMethod(*):*] in type de.quantummaid.reflectmaid.AbstractType")
        )
    }

    @Test
    fun inheritedMethodCanBeFound() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<InheritingClass>()
        val result = type.query(method("foo"))
        assertThat(result.resolvedMethod.name, `is`("foo"))
    }

    @Test
    fun inheritedFieldCanBeFound() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<InheritingClass>()
        val result = type.query(field("field"))
        assertThat(result.resolvedField.name, `is`("field"))
    }

    @Test
    fun nonPublicMethodsAreIgnored() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val exception = withException<QueryNotFoundException> { type.query(method("privateMethod")) }
        assertThat(
            exception.message,
            containsString("unable to find query [privateMethod(*):*] in type de.quantummaid.reflectmaid.Inner")
        )
    }

    @Test
    fun nonPublicFieldsAreIgnored() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val exception = withException<QueryNotFoundException> { type.query(field("privateField")) }
        assertThat(
            exception.message,
            containsString("unable to find query [privateField:*] in type de.quantummaid.reflectmaid.Inner")
        )
    }

    @Test
    fun setterForFinalFieldCannotBeCreated() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Outer>()
        val result = type.query(field("outerField"))
        val exception = withException<UnsupportedOperationException> { result.createSetter() }
        assertThat(
            exception.message,
            containsString("unable to create setter for field 'private final Inner outerField' because it is final")
        )
    }

    @Test
    fun cannotCreateGetterForMethodWithParameters() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("methodWithParameters"))
        val exception = withException<UnsupportedOperationException> { result.createGetter() }
        assertThat(
            exception.message,
            containsString("cannot create getter from method 'fun methodWithParameters(string: String)' " +
                    "[public final void de.quantummaid.reflectmaid.Inner.methodWithParameters(java.lang.String)]")
        )
    }

    @Test
    fun cannotCreateSetterForMethodWithoutParameters() {
        val reflectMaid = aReflectMaid()
        val type = reflectMaid.resolve<Inner>()
        val result = type.query(method("innerMethod"))
        val exception = withException<UnsupportedOperationException> { result.createSetter() }
        assertThat(
            exception.message,
            containsString("unable to create setter from method 'fun innerMethod(): String' " +
                    "[public final java.lang.String de.quantummaid.reflectmaid.Inner.innerMethod()]")
        )
    }
}

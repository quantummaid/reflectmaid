package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.fromResolvedType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheSpecs {

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
    fun reflectMaidCanProvideRegisteredTypes() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val stringResolvedType = reflectMaid.resolve(String::class)
        val anyResolvedType = reflectMaid.resolve(genericType<List<String>>(List::class, genericType(String::class.java)))

        reflectMaid.resolve(String::class.java)

        val registeredTypes = reflectMaid.registeredTypes()

        assertThat(registeredTypes, hasSize(2))
        assertThat(registeredTypes, contains(stringResolvedType, anyResolvedType))
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
        val resolvedType = ClassType.fromClassWithoutGenerics(String::class.java)
        assertSameReferenceGetsReturned { fromResolvedType<String>(resolvedType) }
    }

    @Test
    fun sameResolvedTypeReferenceGetsReturnedForSameWildcardBasedGenericType() {
        assertSameReferenceGetsReturned { wildcard() }
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
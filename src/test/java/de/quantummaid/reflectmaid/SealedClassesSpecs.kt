package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

sealed class SealedClass

class SubClass0 : SealedClass()
class SubClass1 : SealedClass()
sealed class SealedSubClass : SealedClass()
class SubSubClass0 : SealedSubClass()
class SubSubClass1 : SealedSubClass()

class SealedClassesSpecs {

    @Test
    fun sealedSubclassesCanBeFound() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<SealedClass>()
        val sealedSubclasses = resolvedType.sealedSubclasses()
        assertThat(sealedSubclasses.map { it.simpleDescription() }, contains("SubClass0", "SubClass1", "SealedSubClass"))
    }

    @Test
    fun nestedSealedSubclassesCanBeFound() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<SealedClass>()
        val sealedSubclasses = resolvedType.sealedSubclasses()
        val nestedSealedSubclasses = sealedSubclasses.flatMap { it.sealedSubclasses() }
        assertThat(nestedSealedSubclasses.map { it.simpleDescription() }, contains("SubSubClass0", "SubSubClass1"))
    }

    @Test
    fun sealedSubclassesForJavaClassesAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<InputStream>()
        assertThat(resolvedType.sealedSubclasses(), empty())
    }

    @Test
    fun sealedSubclassesForArraysAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<Array<SealedClass>>()
        assertTrue(resolvedType.isArray)
        assertThat(resolvedType.sealedSubclasses(), empty())
    }

    @Test
    fun sealedSubclassesForWildcardsAreEmpty() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        assertThat(resolvedType.sealedSubclasses(), empty())
    }
}
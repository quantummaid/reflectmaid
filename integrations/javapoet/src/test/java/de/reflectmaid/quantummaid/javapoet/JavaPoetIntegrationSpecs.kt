package de.reflectmaid.quantummaid.javapoet

import de.quantummaid.reflectmaid.GenericType.Companion.wildcard
import de.quantummaid.reflectmaid.ReflectMaid
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class JavaPoetIntegrationSpecs {

    @Test
    fun classNameOfNormalClassCanBeMapped() {
        assertMapping<String>("java.lang.String")
    }

    @Test
    fun classNameOfClassWithTypeParametersCanBeMapped() {
        assertMapping<List<String>>("java.util.List<java.lang.String>")
    }

    @Test
    fun classNameOfBoxedPrimitiveCanBeMapped() {
        assertMapping<Int>("java.lang.Integer")
    }

    @Test
    fun classNameOfPrimitiveCanBeMapped() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<List<Any>>()
        val sizeMethod = resolvedType.methods().first { it.name() == "size" }
        val intType = sizeMethod.returnType!!
        val typeName = intType.toTypeName()
        assertThat(typeName.toString(), `is`("int"))
    }

    @Test
    fun classNameOfArrayCanBeMapped() {
        assertMapping<Array<String>>("java.lang.String[]")
    }

    @Test
    fun classNameOfWildcardCanBeMapped() {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve(wildcard())
        val typeName = resolvedType.toTypeName()
        assertThat(typeName.toString(), `is`("java.lang.Object"))
    }

    private inline fun <reified T: Any> assertMapping(asString: String) {
        val reflectMaid = ReflectMaid.aReflectMaid()
        val resolvedType = reflectMaid.resolve<T>()
        val typeName = resolvedType.toTypeName()
        assertThat(typeName.toString(), `is`(asString))
    }
}
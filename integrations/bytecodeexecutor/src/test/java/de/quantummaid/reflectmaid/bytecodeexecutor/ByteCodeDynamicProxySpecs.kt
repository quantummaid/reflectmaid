package de.quantummaid.reflectmaid.bytecodeexecutor

import de.quantummaid.reflectmaid.ReflectMaid
import de.quantummaid.reflectmaid.bytecodeexecutor.ByteCodeExecutorFactory.Companion.byteCodeExecutorFactory
import de.quantummaid.reflectmaid.createDynamicProxy
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

interface MyInterface {
    fun call(parameter: String): String
}

interface MyTypedInterface<T> {
    fun call(parameter0: T, parameter1: T): T
}

interface MyMultiMethodInterface {
    fun method0(): String

    fun method1(): String
}

interface MyVoidInterface {
    fun call(parameter: MutableList<String>)
}

class ByteCodeDynamicProxySpecs {

    @Test
    fun dynamicProxyCanBeCreated() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val proxy = reflectMaid.createDynamicProxy<MyInterface> { _, parameters ->
            "found: " + parameters[0]
        }

        val result = proxy.call("foooo")
        assertThat(result, `is`("found: foooo"))
    }

    @Test
    fun dynamicProxyInterfaceCanHaveTypeVariables() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())

        val stringProxy = reflectMaid.createDynamicProxy<MyTypedInterface<String>> { _, parameters ->
            parameters[0] as String + parameters[1] as String
        }
        val stringResult = stringProxy.call("foo", "bar")
        assertThat(stringResult, `is`("foobar"))

        val intProxy = reflectMaid.createDynamicProxy<MyTypedInterface<Int>> { _, parameters ->
            parameters[0] as Int + parameters[1] as Int
        }
        val intResult = intProxy.call(1, 2)
        assertThat(intResult, `is`(3))
    }

    @Test
    fun dynamicProxyInterfaceCanHaveMultipleMethods() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val proxy = reflectMaid.createDynamicProxy<MyMultiMethodInterface> { method, _ ->
            when (method.name()) {
                "method0" -> "foo"
                "method1" -> "bar"
                else -> throw UnsupportedOperationException()
            }
        }

        assertThat(proxy.method0(), `is`("foo"))
        assertThat(proxy.method1(), `is`("bar"))
    }

    @Test
    fun dynamicProxyInterfaceCanHaveVoidMethod() {
        val reflectMaid = ReflectMaid.aReflectMaid(byteCodeExecutorFactory())
        val proxy = reflectMaid.createDynamicProxy<MyVoidInterface> { _, parameters ->
            @Suppress("UNCHECKED_CAST") val list = parameters[0] as MutableList<String>
            list.add("foo")
        }
        val list = ArrayList<String>()
        proxy.call(list)
        assertThat(list[0], `is`("foo"))
    }
}
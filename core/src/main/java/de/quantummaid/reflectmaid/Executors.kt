package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod

interface Executor {
    fun execute(instance: Any?, parameters: List<Any?>): Any?
}

interface Setter {
    fun set(instance: Any?, value: Any?)
}

interface Getter {
    fun get(instance: Any?): Any?
}

interface ExecutorFactory {
    fun createMethodExecutor(method: ResolvedMethod): Executor
    fun createConstructorExecutor(constructor: ResolvedConstructor): Executor
    fun createFieldGetter(field: ResolvedField): Getter
    fun createFieldSetter(field: ResolvedField): Setter
}

class ReflectionExecutorFactory : ExecutorFactory {
    override fun createMethodExecutor(method: ResolvedMethod) = ReflectionMethodExecutor(method)
    override fun createConstructorExecutor(constructor: ResolvedConstructor) = ReflectionConstructorExecutor(constructor)
    override fun createFieldGetter(field: ResolvedField) = ReflectionFieldGetter(field)
    override fun createFieldSetter(field: ResolvedField) = ReflectionFieldSetter(field)
}

class ReflectionMethodExecutor(private val method: ResolvedMethod) : Executor {

    override fun execute(instance: Any?, parameters: List<Any?>): Any? {
        return method.method.invoke(instance, *parameters.toTypedArray())
    }
}

class ReflectionConstructorExecutor(private val constructor: ResolvedConstructor) : Executor {

    override fun execute(instance: Any?, parameters: List<Any?>): Any? {
        return constructor.constructor.newInstance(*parameters.toTypedArray())
    }
}

class ReflectionFieldGetter(private val field: ResolvedField) : Getter {
    override fun get(instance: Any?): Any? {
        return field.field.get(instance)
    }
}

class ReflectionFieldSetter(private val field: ResolvedField) : Setter {

    override fun set(instance: Any?, value: Any?) {
        field.field.set(instance, value)
    }
}

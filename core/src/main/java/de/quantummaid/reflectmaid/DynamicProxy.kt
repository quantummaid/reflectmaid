package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

fun interface ProxyHandler {
    fun invoke(method: ResolvedMethod, parameters: List<Any?>): Any?
}

inline fun <reified T : Any> ReflectMaid.createDynamicProxy(handler: ProxyHandler): T {
    val resolvedType = resolve<T>()
    return createDynamicProxy(resolvedType, handler)
}

fun <T : Any> ReflectMaid.createDynamicProxy(facadeInterface: Class<T>, handler: ProxyHandler): T {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxy(resolvedType, handler)
}

fun <T : Any> ReflectMaid.createDynamicProxy(facadeInterface: KClass<T>, handler: ProxyHandler): T {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxy(resolvedType, handler)
}

fun <T> ReflectMaid.createDynamicProxy(facadeInterface: GenericType<T>, handler: ProxyHandler): T {
    val resolvedType = resolve(facadeInterface)
    return createDynamicProxy(resolvedType, handler)
}

fun <T> ReflectMaid.createDynamicProxy(facadeInterface: ResolvedType, handler: ProxyHandler): T {
    if (!facadeInterface.isInterface) {
        throw DynamicProxyException(
            "type '${facadeInterface.description()}' needs to be an interface to be used " +
                    "as a dynamic proxy facade"
        )
    }
    return executorFactory.createDynamicProxy(facadeInterface, handler)
}

fun <T> createDynamicProxyUsingInvocationHandler(facadeInterface: ResolvedType, handler: ProxyHandler): T {
    val methods = facadeInterface.methods()
        .map { it.method to it }
        .toMap()
    val invocationHandler = InternalInvocationHandler(handler, methods)
    val classLoader = handler::class.java.classLoader
    val assignableType = facadeInterface.assignableType()
    val proxyInstance = Proxy.newProxyInstance(
        classLoader,
        arrayOf(assignableType),
        invocationHandler
    )
    @Suppress("UNCHECKED_CAST")
    return proxyInstance as T
}

internal class InternalInvocationHandler(
    private val handler: ProxyHandler,
    private val methods: Map<Method, ResolvedMethod>
) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val resolvedMethod = methods[method]!!
        val parameters = args?.toList() ?: emptyList<Any?>()
        return handler.invoke(resolvedMethod, parameters)
    }
}

class DynamicProxyException(message: String) : RuntimeException(message)
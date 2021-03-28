package de.quantummaid.reflectmaid

import kotlin.reflect.KClass

sealed class GenericType<T> {
    companion object {
        @JvmStatic
        fun <T> genericType(type: Class<T>): GenericType<T> {
            return genericType(type, emptyList())
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, typeVariables: List<GenericType<*>>): GenericType<T> {
            return GenericTypeFromClass(type, typeVariables)
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, vararg typeVariables: GenericType<*>): GenericType<T> {
            return genericType(type, typeVariables.toList())
        }

        @JvmStatic
        fun <T> genericType(type: Class<*>, vararg typeVariables: Class<*>): GenericType<T> {
            return GenericTypeFromClass(type, typeVariables.map { genericType(it) })
        }

        @JvmStatic
        fun <T> genericType(typeToken: TypeToken<T>): GenericType<T> {
            return GenericTypeFromToken(typeToken)
        }

        inline fun <reified T : Any> genericType(): GenericType<T> {
            return genericType(object : TypeToken<T>() {})
        }

        fun <T : Any> genericType(type: KClass<T>): GenericType<T> {
            return genericType(type, emptyList())
        }

        fun <T : Any> genericType(type: KClass<*>, typeVariables: List<GenericType<*>>): GenericType<T> {
            return GenericTypeFromKClass(type, typeVariables)
        }

        fun <T : Any> genericType(type: KClass<*>, vararg typeVariables: GenericType<*>): GenericType<T> {
            return genericType(type, typeVariables.toList())
        }

        fun <T : Any> genericType(type: KClass<*>, vararg typeVariables: KClass<*>): GenericType<T> {
            return genericType(type, typeVariables.map { genericType(it) })
        }

        @JvmStatic
        fun wildcard(): GenericType<*> {
            return GenericTypeWildcard()
        }

        @JvmStatic
        fun <T : Any> fromResolvedType(resolvedType: ResolvedType): GenericType<T> {
            return GenericTypeFromResolvedType(resolvedType)
        }
    }
}

data class GenericTypeFromClass<T>(val type: Class<*>,
                                   val typeVariables: List<GenericType<*>>) : GenericType<T>()

data class GenericTypeFromKClass<T : Any>(val kClass: KClass<*>,
                                          val typeVariables: List<GenericType<*>>) : GenericType<T>()

data class GenericTypeFromToken<T>(val typeToken: TypeToken<T>) : GenericType<T>()

class GenericTypeWildcard : GenericType<Any>() {
    override fun equals(other: Any?): Boolean {
        return other is GenericTypeWildcard
    }

    override fun hashCode(): Int {
        return 1
    }
}

data class GenericTypeFromResolvedType<T>(val resolvedType: ResolvedType) : GenericType<T>()
package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.resolvedtype.ArrayType.Companion.fromArrayClass
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithoutGenerics
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.WildcardedType
import java.util.stream.Collectors
import kotlin.reflect.KClass

class ReflectMaid(private val cache: ReflectionCache,
                  val executorFactory: ExecutorFactory) {

    fun resolve(type: Class<*>): ResolvedType {
        val genericType = genericType(type)
        return resolve(genericType)
    }

    fun resolve(type: KClass<*>): ResolvedType {
        val genericType = genericType(type)
        return resolve(genericType)
    }

    inline fun <reified T : Any> resolve(): ResolvedType {
        val genericType = genericType<T>()
        return resolve(genericType)
    }

    fun resolve(genericType: GenericType<*>): ResolvedType {
        return cache.lookUp(genericType) { resolveInternal(it) }
    }

    fun registeredTypes(): Collection<ResolvedType> {
        return cache.registeredResolvedTypes()
    }

    private fun resolveInternal(genericType: GenericType<*>): ResolvedType {
        return when (genericType) {
            is GenericTypeFromClass -> resolveClass(genericType)
            is GenericTypeFromToken -> {
                resolveFromTypeToken(genericType.typeToken)
            }
            is GenericTypeFromKClass -> {
                return resolve(GenericTypeFromClass<Any>(genericType.kClass.java, genericType.typeVariables))
            }
            is GenericTypeWildcard -> {
                return WildcardedType.wildcardType()
            }
            is GenericTypeFromResolvedType -> {
                return genericType.resolvedType
            }
            is GenericTypeFromReflectionType -> {
                val (type, context) = genericType
                resolveType(this, type, context)
            }
        }
    }

    private fun resolveFromTypeToken(typeToken: TypeToken<*>): ResolvedType {
        val subclass: Class<*> = typeToken.javaClass
        val genericSupertype = subclass.genericSuperclass
        val subclassType = fromClassWithoutGenerics(this, subclass)
        val interfaceType = resolve(fromReflectionType<Any>(genericSupertype, subclassType)) as ClassType
        return interfaceType.typeParameter(TypeVariableName.typeVariableName("T"))
    }

    private fun resolveClass(genericType: GenericTypeFromClass<*>): ResolvedType {
        val (type, typeVariables) = genericType
        if (type.isArray) {
            return fromArrayClass(this, type)
        }
        val resolvedParameters = typeVariables
                .map { resolveInternal(it) }
        return resolve(type, resolvedParameters)
    }

    private fun resolve(type: Class<*>, variableValues: List<ResolvedType>): ResolvedType {
        val variableNames = TypeVariableName.typeVariableNamesOf(type)
        validateVariablesSameSizeAsVariableNames(type, variableNames, variableValues)
        val resolvedParameters: MutableMap<TypeVariableName, ResolvedType> = HashMap(variableValues.size)
        for (i in variableNames.indices) {
            val name: TypeVariableName = variableNames[i]
            val value = variableValues[i]
            resolvedParameters[name] = value
        }
        return if (resolvedParameters.isEmpty()) {
            fromClassWithoutGenerics(this, type)
        } else {
            ClassType.fromClassWithGenerics(this, type, resolvedParameters)
        }
    }

    private fun validateVariablesSameSizeAsVariableNames(type: Class<*>,
                                                         variableNames: List<TypeVariableName>,
                                                         variableValues: List<ResolvedType>) {
        if (variableValues.size != variableNames.size) {
            val variables = variableNames.stream()
                    .map { it.name }
                    .collect(Collectors.joining(", ", "[", "]"))
            throw GenericTypeException(
                    "type '${type.name}' contains the following type variables that need " +
                            "to be filled in in order to create a ${GenericType::class.java.simpleName} object: ${variables}"
            )
        }
    }

    companion object {
        @JvmStatic
        fun aReflectMaid(): ReflectMaid {
            val executorFactory = ReflectionExecutorFactory()
            return ReflectMaid(ReflectionCache(), executorFactory)
        }

        @JvmStatic
        fun aReflectMaid(executorFactory: ExecutorFactory): ReflectMaid {
            return ReflectMaid(ReflectionCache(), executorFactory)
        }
    }
}

class GenericTypeException(message: String) : RuntimeException(message)
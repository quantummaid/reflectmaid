package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.fromReflectionType
import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.exceptions.GenericTypeException
import de.quantummaid.reflectmaid.resolvedtype.ArrayType.Companion.fromArrayClass
import de.quantummaid.reflectmaid.resolvedtype.ClassType
import de.quantummaid.reflectmaid.resolvedtype.ClassType.Companion.fromClassWithoutGenerics
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.WildcardedType
import java.util.stream.Collectors
import kotlin.reflect.KClass

class ReflectMaid(private val cache: ReflectionCache) {

    fun resolve(type: Class<*>): ResolvedType {
        val genericType = genericType(type)
        return resolve(genericType)
    }

    fun resolve(type: KClass<*>): ResolvedType {
        val genericType = genericType(type)
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
                val subclass: Class<*> = genericType.typeToken.javaClass
                val genericSupertype = subclass.genericSuperclass
                val subclassType = fromClassWithoutGenerics(this, subclass)
                val interfaceType = resolve(fromReflectionType<Any>(genericSupertype, subclassType)) as ClassType
                interfaceType.typeParameter(TypeVariableName.typeVariableName("T"))
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
                    .map { obj: TypeVariableName -> obj.name() }
                    .collect(Collectors.joining(", ", "[", "]"))
            throw GenericTypeException.genericTypeException(
                    "type '${type.name}' contains the following type variables that need " +
                            "to be filled in in order to create a ${GenericType::class.java.simpleName} object: ${variables}"
            )
        }
    }

    companion object {
        @JvmStatic
        fun aReflectMaid(): ReflectMaid {
            return ReflectMaid(ReflectionCache())
        }
    }
}
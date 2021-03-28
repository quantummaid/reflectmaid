package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.GenericType.Companion.genericType
import de.quantummaid.reflectmaid.WildcardedType.wildcardType
import de.quantummaid.reflectmaid.cache.ReflectMaidCache
import de.quantummaid.reflectmaid.unresolved.UnresolvedType
import lombok.AccessLevel
import lombok.EqualsAndHashCode
import lombok.RequiredArgsConstructor
import lombok.ToString
import java.util.stream.Collectors
import kotlin.reflect.KClass

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ReflectMaid(private val cache: ReflectMaidCache) {

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
            is GenericTypeFromClass -> {
                val (type, typeVariables) = genericType
                val unresolvedType = UnresolvedType.unresolvedType(type)
                val typeVariableNames = unresolvedType.typeVariables()
                if (typeVariables.isEmpty() && typeVariableNames.isNotEmpty()) {
                    val variables = typeVariableNames.stream()
                            .map { obj: TypeVariableName -> obj.name() }
                            .collect(Collectors.joining(", ", "[", "]"))
                    throw GenericTypeException.genericTypeException(
                            "type '${type.name}' contains the following type variables that need " +
                                    "to be filled in in order to create a ${GenericType::class.java.simpleName} object: ${variables}"
                    )
                }
                val resolvedParameters = typeVariables
                        .map { resolveInternal(it) }
                unresolvedType.resolve(resolvedParameters)
            }
            is GenericTypeFromToken -> {
                val subclass: Class<*> = genericType.typeToken.javaClass
                val genericSupertype = subclass.genericSuperclass
                val subclassType = ClassType.fromClassWithoutGenerics(subclass)
                val interfaceType = TypeResolver.resolveType(genericSupertype, subclassType) as ClassType
                interfaceType.typeParameter(TypeVariableName.typeVariableName("T"))
            }
            is GenericTypeFromKClass -> {
                return resolve(GenericTypeFromClass<Any>(genericType.kClass.java, genericType.typeVariables))
            }
            is GenericTypeWildcard -> {
                return wildcardType()
            }
            is GenericTypeFromResolvedType -> {
                return genericType.resolvedType
            }
        }
    }

    companion object {
        @JvmStatic
        fun aReflectMaid(): ReflectMaid {
            return ReflectMaid(ReflectMaidCache())
        }
    }
}
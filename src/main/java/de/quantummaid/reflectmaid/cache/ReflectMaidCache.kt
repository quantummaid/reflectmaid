package de.quantummaid.reflectmaid.cache

import de.quantummaid.reflectmaid.GenericType
import de.quantummaid.reflectmaid.ResolvedType

class ReflectMaidCache {
    private val map: MutableMap<GenericType<*>, ResolvedType> = LinkedHashMap()

    fun lookUp(genericType: GenericType<*>, default: (GenericType<*>) -> ResolvedType): ResolvedType {
        if (map.containsKey(genericType)) {
            return map[genericType]!!
        }
        val newResolvedType = default.invoke(genericType)
        val resolvedTypeToBePutInMap = findInValues(newResolvedType) ?: newResolvedType
        map[genericType] = resolvedTypeToBePutInMap
        return resolvedTypeToBePutInMap
    }

    fun registeredResolvedTypes(): Collection<ResolvedType> = map.values.distinct()

    private fun findInValues(resolvedType: ResolvedType): ResolvedType? {
        return map.values.find { it == resolvedType }
    }
}
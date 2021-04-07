package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.resolvedtype.ResolvedType

class ReflectionCache {
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
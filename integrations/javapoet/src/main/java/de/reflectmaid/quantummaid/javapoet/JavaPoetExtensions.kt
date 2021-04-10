package de.reflectmaid.quantummaid.javapoet

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType

fun ResolvedType.toTypeName(): TypeName {
    val assignableType = assignableType()
    val className = TypeName.get(assignableType)
    if (className !is ClassName) {
        return className
    }
    val typeParameters = typeParameters()
    if (typeParameters.isEmpty()) {
        return className
    }
    val typeVariables = typeParameters
            .map { it.toTypeName() }
    return ParameterizedTypeName.get(className, *typeVariables.toTypedArray())
}

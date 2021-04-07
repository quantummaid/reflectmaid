package de.quantummaid.reflectmaid.languages

data class ParameterData(val name: String, val type: String)

interface Language {

    companion object {
        val KOTLIN = Kotlin()
        val JAVA = Java()
    }

    fun wildcard(): String

    fun array(componentType: String): String

    fun method(name: String, parameters: List<ParameterData>, returnType: String?): String
}

class Kotlin : Language {

    override fun wildcard() = "*"

    override fun array(componentType: String) = "Array<$componentType>"

    override fun method(name: String, parameters: List<ParameterData>, returnType: String?): String {
        val parametersString = parameters.joinToString { "${it.name}: ${it.type}" }
        val returnTypeDescription = returnType?.let { ": $it" } ?: ""
        return "fun ${name}($parametersString)$returnTypeDescription"
    }
}

class Java : Language {

    override fun wildcard() = "?"

    override fun array(componentType: String) = "$componentType[]"

    override fun method(name: String, parameters: List<ParameterData>, returnType: String?): String {
        val parametersString = parameters.joinToString { "${it.type} ${it.name}" }
        val returnTypeDescription = returnType ?: "void"
        return "$returnTypeDescription ${name}($parametersString)"
    }
}
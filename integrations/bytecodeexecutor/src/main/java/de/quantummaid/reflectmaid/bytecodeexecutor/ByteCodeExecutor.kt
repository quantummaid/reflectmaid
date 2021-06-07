/**
 * Copyright (c) 2021 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.quantummaid.reflectmaid.bytecodeexecutor

import com.squareup.javapoet.*
import de.quantummaid.reflectmaid.*
import de.quantummaid.reflectmaid.bytecodeexecutor.FieldsAndConstructor.Companion.empty
import de.quantummaid.reflectmaid.bytecodeexecutor.compilation.InMemoryCompiler
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedParameter
import de.reflectmaid.quantummaid.javapoet.toTypeName
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.*
import kotlin.reflect.KClass

class ByteCodeExecutorFactory(private val generator: Generator) : ExecutorFactory {

    companion object {
        @JvmStatic
        fun byteCodeExecutorFactory(): ByteCodeExecutorFactory {
            return byteCodeExecutorFactory("de.reflectmaid.generated")
        }

        @JvmStatic
        fun byteCodeExecutorFactory(targetPackage: String): ByteCodeExecutorFactory {
            return ByteCodeExecutorFactory(Generator(targetPackage))
        }
    }

    override fun createMethodExecutor(method: ResolvedMethod): Executor {
        val builder = overrideMethod("execute")
            .returns(Any::class.java)
            .addParameter(Any::class.java, "instance", FINAL)
            .addParameter(List::class.java, "parameters", FINAL)
        val declaringClass = method.declaringType.toTypeName()
        builder.addStatement("final \$T typedInstance = (\$T) instance", declaringClass, declaringClass)
        val parametersString = buildParameters(method.parameters, builder)
        val methodCall = "typedInstance.${method.name()}($parametersString)"
        if (method.returnType != null) {
            builder.addStatement("return $methodCall")
        } else {
            builder.addStatement(methodCall)
            builder.addStatement("return null")
        }
        return generator.createInstance(Executor::class, builder.build()) as Executor
    }

    override fun createConstructorExecutor(constructor: ResolvedConstructor): Executor {
        val builder = overrideMethod("execute")
            .addModifiers(PUBLIC)
            .returns(Any::class.java)
            .addParameter(Any::class.java, "instance", FINAL)
            .addParameter(List::class.java, "parameters", FINAL)
        val parametersString = buildParameters(constructor.parameters, builder)
        builder.addStatement("return new \$T($parametersString)", constructor.declaringType.toTypeName())
        return generator.createInstance(Executor::class, builder.build()) as Executor
    }

    override fun createFieldGetter(field: ResolvedField): Getter {
        val getterMethod = overrideMethod("get")
            .returns(Any::class.java)
            .addParameter(Any::class.java, "instance", FINAL)
            .addStatement("return ((\$T)instance).${field.name}", field.declaringType.toTypeName())
            .build()
        return generator.createInstance(Getter::class, getterMethod) as Getter
    }

    override fun createFieldSetter(field: ResolvedField): Setter {
        val setterMethod = overrideMethod("set")
            .returns(Void.TYPE)
            .addParameter(Any::class.java, "instance", FINAL)
            .addParameter(Any::class.java, "value", FINAL)
            .addStatement(
                "((\$T)instance).${field.name} = (\$T) value",
                field.declaringType.toTypeName(),
                field.type.toTypeName()
            )
            .build()
        return generator.createInstance(Setter::class, setterMethod) as Setter
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> createDynamicProxyFactory(facadeInterface: ResolvedType): ProxyFactory<T> {
        return ProxyFactory { handler ->
            val proxyHandlerName = "proxyHandler"

            val methods = facadeInterface.methods()
            val methodFields = methods.map { Field(it.name() + "Method", TypeName.get(ResolvedMethod::class.java)) }
            val fieldsAndConstructor = FieldsAndConstructor.createFieldsAndConstructor(
                listOf(
                    Field(proxyHandlerName, TypeName.get(ProxyHandler::class.java))
                ) + methodFields
            )

            val methodSpecs = methods.map { method ->
                val methodSpec = overrideMethod(method.name())
                val returnType = method.returnType
                val returnTypeName = returnType?.toTypeName() ?: TypeName.VOID
                methodSpec.returns(returnTypeName)
                val listType = ParameterizedTypeName.get(List::class.java, Any::class.java)

                method.parameters.forEach {
                    methodSpec.addParameter(it.type.toTypeName(), it.name(), Modifier.FINAL)
                }

                val parametersForList = method.parameters.joinToString { it.name() }
                methodSpec.addStatement("final \$T list = \$T.of($parametersForList)", listType, List::class.java)
                methodSpec.addStatement(
                    "final \$T returnValue = $proxyHandlerName.invoke(${method.name() + "Method"}, list)",
                    Any::class.java
                )
                if (returnType != null) {
                    methodSpec.addStatement("return (\$T) returnValue", returnType.toTypeName())
                }

                methodSpec.build()
            }
            val compiledClass = generator.createClass(facadeInterface.toTypeName(), methodSpecs, fieldsAndConstructor)

            val declaredConstructor = compiledClass.getDeclaredConstructor(
                ProxyHandler::class.java, *methods.map { ResolvedMethod::class.java }.toTypedArray()
            )
            declaredConstructor.newInstance(handler, *methods.toTypedArray()) as T
        }
    }
}

class Generator(private val targetPackage: String) {
    fun createInstance(implements: KClass<*>, methodSpec: MethodSpec): Any {
        val typeName = TypeName.get(implements.java)
        val compiledClass = createClass(typeName, listOf(methodSpec))
        val declaredConstructor = compiledClass.getDeclaredConstructor()
        return declaredConstructor.newInstance()
    }

    fun createClass(
        implements: TypeName,
        methodSpecs: List<MethodSpec>,
        fieldsAndConstructor: FieldsAndConstructor = empty()
    ): Class<*> {
        val className = createClassName()
        val typeSpec = TypeSpec.classBuilder(className)
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(implements)
        methodSpecs.forEach { typeSpec.addMethod(it) }
        fieldsAndConstructor.apply(typeSpec)
        val javaFile = JavaFile.builder(targetPackage, typeSpec.build()).build()
        val stringBuilder = StringBuilder()
        javaFile.writeTo(stringBuilder)
        val program = stringBuilder.toString()
        val compiler = InMemoryCompiler.createInMemoryCompiler()
        return compiler.compileAndLoad(program, "$targetPackage.$className")
    }

    private var counter = 0
    private fun createClassName(): String {
        val className = "GeneratedClass$counter"
        counter += 1
        return className
    }
}

fun overrideMethod(name: String): MethodSpec.Builder {
    return MethodSpec.methodBuilder(name)
        .addAnnotation(Override::class.java)
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings::class.java)
                .addMember("value", "\$S", "unchecked")
                .build()
        )
        .addModifiers(PUBLIC)
}

private fun buildParameters(parameters: List<ResolvedParameter>, builder: MethodSpec.Builder): String {
    val parameterNames = ArrayList<String>()
    parameters.forEachIndexed { i, parameter ->
        val parameterType = parameter.type.toTypeName()
        val parameterName = "parameter$i"
        builder.addStatement("final \$T $parameterName = (\$T) parameters.get($i)", parameterType, parameterType)
        parameterNames.add(parameterName)
    }
    return parameterNames.joinToString(separator = ", ")
}

data class FieldsAndConstructor(val fieldSpecs: List<FieldSpec>, val constructor: MethodSpec) {
    companion object {
        fun empty(): FieldsAndConstructor {
            return createFieldsAndConstructor(emptyList())
        }

        fun createFieldsAndConstructor(fields: List<Field>): FieldsAndConstructor {
            val constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
            val fieldSpecs = ArrayList<FieldSpec>()
            fields.forEach { (name, type) ->
                constructorBuilder.addParameter(type, name, FINAL)
                constructorBuilder.addStatement("this.$name = $name")
                fieldSpecs.add(FieldSpec.builder(type, name, PRIVATE, FINAL).build())
            }
            return FieldsAndConstructor(fieldSpecs, constructorBuilder.build())
        }
    }

    fun apply(typeSpec: TypeSpec.Builder) {
        fieldSpecs.forEach { typeSpec.addField(it) }
        typeSpec.addMethod(constructor)
    }
}

data class Field(val name: String, val type: TypeName)


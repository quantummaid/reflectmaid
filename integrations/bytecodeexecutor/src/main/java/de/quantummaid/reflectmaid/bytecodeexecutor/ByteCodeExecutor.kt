package de.quantummaid.reflectmaid.bytecodeexecutor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import de.quantummaid.reflectmaid.Executor
import de.quantummaid.reflectmaid.ExecutorFactory
import de.quantummaid.reflectmaid.Getter
import de.quantummaid.reflectmaid.Setter
import de.quantummaid.reflectmaid.bytecodeexecutor.compilation.InMemoryCompiler
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedParameter
import de.reflectmaid.quantummaid.javapoet.toTypeName
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
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
                .addStatement("((\$T)instance).${field.name} = (\$T) value",
                        field.declaringType.toTypeName(),
                        field.type.toTypeName()
                )
                .build()
        return generator.createInstance(Setter::class, setterMethod) as Setter
    }
}

class Generator(private val targetPackage: String) {
    fun createInstance(implements: KClass<*>,
                       methodSpec: MethodSpec): Any {
        val className = createClassName()
        val typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(PUBLIC, FINAL)
                .addSuperinterface(implements.java)
                .addMethod(methodSpec)
                .build()
        val javaFile = JavaFile.builder(targetPackage, typeSpec).build()
        val stringBuilder = StringBuilder()
        javaFile.writeTo(stringBuilder)
        val program = stringBuilder.toString()
        val compiler = InMemoryCompiler.createInMemoryCompiler()
        val compiledClass = compiler.compileAndLoad(program, "$targetPackage.$className")
        val declaredConstructor = compiledClass.getDeclaredConstructor()
        return declaredConstructor.newInstance()
    }

    private var counter = 0
    private fun createClassName(): String {
        val className = "GeneratedClass$counter"
        counter += 1
        return className
    }
}

private fun overrideMethod(name: String): MethodSpec.Builder {
    return MethodSpec.methodBuilder(name)
            .addAnnotation(Override::class.java)
            .addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java)
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
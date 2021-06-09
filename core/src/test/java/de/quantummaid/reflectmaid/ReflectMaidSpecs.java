/*
 * Copyright (c) 2020 Richard Hauswald - https://quantummaid.de/.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.quantummaid.reflectmaid;

import de.quantummaid.reflectmaid.resolvedtype.ClassType;
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType;
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedConstructor;
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField;
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedMethod;
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedParameter;
import de.quantummaid.reflectmaid.types.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import static de.quantummaid.reflectmaid.GenericType.genericType;
import static de.quantummaid.reflectmaid.GenericType.wildcard;
import static de.quantummaid.reflectmaid.ReflectMaid.aReflectMaid;
import static de.quantummaid.reflectmaid.TypeVariableName.typeVariableName;
import static de.quantummaid.reflectmaid.util.ExceptionThrowingLambda.withException;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public final class ReflectMaidSpecs {

    @Test
    public void typeWithoutTypeVariables() {
        final ReflectMaid reflectMaid = aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(TestType.class);
        assertThat(resolvedType.assignableType(), is(TestType.class));
        assertThat(resolvedType.description(), is("de.quantummaid.reflectmaid.types.TestType"));
        assertThat(resolvedType.simpleDescription(), is("TestType"));
        assertThat(resolvedType.isAbstract(), is(false));
        assertThat(resolvedType.isPublic(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(true));
        assertThat(resolvedType.isInterface(), is(false));
        assertThat(resolvedType.isWildcard(), is(false));
        assertThat(resolvedType.isAnonymousClass(), is(false));
        assertThat(resolvedType.isAnnotation(), is(false));
        assertThat(resolvedType.isInnerClass(), is(false));
        assertThat(resolvedType.isLocalClass(), is(false));
        assertThat(resolvedType.isStatic(), is(false));

        assertThat(resolvedType, instanceOf(ClassType.class));
        final ClassType classType = (ClassType) resolvedType;
        assertThat(classType.typeParameters(), empty());

        assertThat(classType.constructors(), hasSize(1));
        final ResolvedConstructor constructor = classType.constructors().get(0);
        assertThat(constructor.isPublic(), is(true));
        assertThat(constructor.getParameters(), hasSize(0));
        assertThat(constructor.describe(), is("public de.quantummaid.reflectmaid.types.TestType()"));
        assertThat(constructor.getConstructor().getName(), is("de.quantummaid.reflectmaid.types.TestType"));

        assertThat(classType.methods(), hasSize(1));
        final ResolvedMethod method = classType.methods().get(0);
        assertThat(method.isPublic(), is(true));
        assertThat(method.describe(), is("'String method()' [public java.lang.String de.quantummaid.reflectmaid.types.TestType.method()]"));
        assertThat(method.getParameters(), hasSize(0));
        assertThat(method.name(), is("method"));
        assertThat(method.returnType().isPresent(), is(true));
        assertThat(method.getMethod().getName(), is("method"));

        assertThat(classType.fields(), hasSize(1));
        final ResolvedField field = classType.fields().get(0);
        assertThat(field.isPublic(), is(false));
        assertThat(field.isStatic(), is(false));
        assertThat(field.isTransient(), is(false));
        assertThat(field.describe(), is("private String field"));
        assertThat(field.getName(), is("field"));
        assertThat(field.getType().simpleDescription(), is("String"));
        assertThat(field.getField().getName(), is("field"));
    }

    @Test
    public void typeWithTypeVariables() {
        final ReflectMaid reflectMaid = aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(genericType(TestTypeWithTypeVariables.class, String.class));

        assertThat(resolvedType.assignableType(), is(TestTypeWithTypeVariables.class));
        assertThat(resolvedType.description(), is("de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables<java.lang.String>"));
        assertThat(resolvedType.simpleDescription(), is("TestTypeWithTypeVariables<String>"));
        assertThat(resolvedType.isAbstract(), is(false));
        assertThat(resolvedType.isPublic(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(true));
        assertThat(resolvedType.isInterface(), is(false));
        assertThat(resolvedType.isWildcard(), is(false));
        assertThat(resolvedType.isAnonymousClass(), is(false));
        assertThat(resolvedType.isAnnotation(), is(false));
        assertThat(resolvedType.isInnerClass(), is(false));
        assertThat(resolvedType.isLocalClass(), is(false));
        assertThat(resolvedType.isStatic(), is(false));

        assertThat(resolvedType, instanceOf(ClassType.class));
        final ClassType classType = (ClassType) resolvedType;
        assertThat(classType.typeParameters(), hasSize(1));
        final ResolvedType typeParameter = classType.typeParameter(typeVariableName("A"));
        assertThat(typeParameter.simpleDescription(), is("String"));

        final Exception exception1 = withException(() -> classType.typeParameter(typeVariableName("foo")));
        assertThat(exception1.getMessage(), is("No type parameter with the name: foo"));

        final Exception exception2 = withException(() -> reflectMaid.resolve(TestTypeWithTypeVariables.class));
        assertThat(exception2.getMessage(), is("type 'de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables' contains the following type variables " +
                "that need to be filled in in order to create a GenericType object: [A]"));

        assertThat(classType.methods(), hasSize(3));
        final ResolvedMethod parameterizedMethod = classType.methods().stream()
                .filter(resolvedMethod -> resolvedMethod.name().equals("foo"))
                .findFirst()
                .orElseThrow();
        assertThat(parameterizedMethod.describe(), is("'void foo(TestTypeWithTypeVariables<String> other)' " +
                "[public void de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables.foo(de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables<A>)]"));
        assertThat(parameterizedMethod.hasParameters(List.of(resolvedType)), is(true));
        assertThat(parameterizedMethod.hasParameters(List.of()), is(false));
        assertThat(parameterizedMethod.hasParameters(List.of(reflectMaid.resolve(String.class))), is(false));

        assertThat(parameterizedMethod.getParameters(), hasSize(1));
        final ResolvedParameter parameter = parameterizedMethod.getParameters().get(0);
        assertThat(parameter.name(), is("other"));
        assertThat(parameter.getType().simpleDescription(), is("TestTypeWithTypeVariables<String>"));
        assertThat(parameter.getParameter().getName(), is("other"));
    }

    @Test
    public void wildcardedType() {
        final ReflectMaid reflectMaid = aReflectMaid();
        final ResolvedType wildcardedType = reflectMaid.resolve(wildcard());
        assertThat(wildcardedType.isWildcard(), is(true));
        assertThat(wildcardedType.description(), is("?"));
        assertThat(wildcardedType.assignableType(), is(Object.class));
        assertThat(wildcardedType.typeParameters(), hasSize(0));
        assertThat(wildcardedType.isAbstract(), is(false));
        assertThat(wildcardedType.isPublic(), is(true));
        assertThat(wildcardedType.isInstantiatable(), is(false));
        assertThat(wildcardedType.isInterface(), is(false));
        assertThat(wildcardedType.isAnonymousClass(), is(false));
        assertThat(wildcardedType.isAnnotation(), is(false));
        assertThat(wildcardedType.isInnerClass(), is(false));
        assertThat(wildcardedType.isLocalClass(), is(false));
        assertThat(wildcardedType.isStatic(), is(false));
    }

    @Test
    public void parameters() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(genericType(TestTypeWithSpecialParameters.class, String.class));

        assertThat(resolvedType.methods(), hasSize(1));
        final ResolvedMethod resolvedMethod = resolvedType.methods().get(0);

        assertThat(resolvedMethod.returnType().isPresent(), is(true));
        final ResolvedType returnType = resolvedMethod.returnType().get();
        assertThat(returnType.simpleDescription(), is("List<Object>"));

        assertThat(resolvedMethod.getParameters(), hasSize(3));
        final ResolvedParameter parameter1 = resolvedMethod.getParameters().get(0);
        assertThat(parameter1.getType().simpleDescription(), is("String[]"));
        final ResolvedParameter parameter2 = resolvedMethod.getParameters().get(1);
        assertThat(parameter2.getType().simpleDescription(), is("String[]"));
        final ResolvedParameter parameter3 = resolvedMethod.getParameters().get(2);
        assertThat(parameter3.getType().simpleDescription(), is("List<?>"));
    }

    @Test
    public void fields() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ClassType classType = (ClassType) reflectMaid.resolve(TypeWithFields.class);
        assertThat(classType.fields(), hasSize(2));
        final ResolvedField staticField = classType.fields().stream()
                .filter(resolvedField -> resolvedField.getName().equals("FIELD_1"))
                .findFirst()
                .orElseThrow();
        assertThat(staticField.describe(), is("public static final String FIELD_1"));
        assertThat(staticField.isStatic(), is(true));

        final ResolvedField transientField = classType.fields().stream()
                .filter(resolvedField -> resolvedField.getName().equals("field2"))
                .findFirst()
                .orElseThrow();
        assertThat(transientField.describe(), is("protected transient String field2"));
        assertThat(transientField.isTransient(), is(true));
        assertThat(transientField.isPublic(), is(false));
    }

    @Test
    public void primitive() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(int.class);
        assertThat(resolvedType.description(), is("int"));
        assertThat(resolvedType.simpleDescription(), is("int"));
        assertThat(resolvedType.isAbstract(), is(false));
    }

    @Test
    public void abstractClass() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(InputStream.class);
        assertThat(resolvedType.isAbstract(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(false));
    }

    @Test
    public void interfaceAsResolvedType() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(Serializable.class);
        assertThat(resolvedType.isInterface(), is(true));
        assertThat(resolvedType.isAbstract(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(false));
    }

    @Test
    public void innerClass() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(InnerClass.class);
        assertThat(resolvedType.isInnerClass(), is(true));
        assertThat(resolvedType.description(), is("de.quantummaid.reflectmaid.ReflectMaidSpecs$InnerClass"));
    }

    @Test
    public void methodsWithUnresolvableTypeVariablesAreIgnored() {
        final ReflectMaid reflectMaid = aReflectMaid();
        final ClassType classType = (ClassType) reflectMaid.resolve(TypeWithUnresolvableTypeVariable.class);
        assertThat(classType.methods(), hasSize(0));
    }

    @Test
    public void syntheticFeaturesAreIgnored() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ClassType classType = (ClassType) reflectMaid.resolve(ReflectMaid.class);
        assertThat(classType.fields(), hasSize(4));
        assertThat(classType.constructors(), hasSize(1));
        assertThat(classType.methods(), hasSize(13));
    }

    @Test
    public void unsupportedJvmFeature() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final ClassType context = (ClassType) reflectMaid.resolve(String.class);
        final GenericType<?> genericType = GenericType.fromReflectionType(new UnsupportedJvmFeature(), context);
        final Exception exception = withException(() -> reflectMaid.resolve(genericType));
        assertThat(exception.getMessage(), is("Unknown 'Type' implementation by class 'class de.quantummaid.reflectmaid.types.UnsupportedJvmFeature' " +
                "on object 'unsupported'"));
    }

    @Test
    public void unresolvedType() {
        final ReflectMaid reflectMaid = ReflectMaid.aReflectMaid();
        final Exception exception = withException(() -> reflectMaid.resolve(TestTypeWithTypeVariables.class));
        assertThat(exception, instanceOf(GenericTypeException.class));

        final ResolvedType resolvedType = reflectMaid.resolve(TestType.class);
        assertThat(resolvedType.simpleDescription(), is("TestType"));
    }

    @Test
    public void genericTypeWithoutTypeVariables() {
        final GenericType<TestType> genericType = genericType(TestType.class);
        final ReflectMaid reflectMaid = aReflectMaid();
        assertThat(reflectMaid.resolve(genericType).simpleDescription(), is("TestType"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void genericTypeWithTypeVariables() {
        Exception exception = null;
        final GenericType<TestTypeWithTypeVariables> genericType1 = genericType(TestTypeWithTypeVariables.class);
        final ReflectMaid reflectMaid = aReflectMaid();
        try {
            reflectMaid.resolve(genericType1);
        } catch (final Exception e) {
            exception = e;
        }
        assertThat(exception, notNullValue());
        assertThat(exception.getMessage(), is("type 'de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables' " +
                "contains the following type variables that need to be filled in in order to create a GenericType object: [A]"));

        final GenericType<TestTypeWithTypeVariables> genericType = genericType(TestTypeWithTypeVariables.class, String.class);
        assertThat(reflectMaid.resolve(genericType).simpleDescription(), is("TestTypeWithTypeVariables<String>"));
    }

    @Test
    public void wildcardsWithSingleUpperBoundAreNormalized() {
        final GenericType<TypeWithFieldWithWildcardGenericWithSingleUpperBound> genericType =
                genericType(TypeWithFieldWithWildcardGenericWithSingleUpperBound.class);
        final ReflectMaid reflectMaid = aReflectMaid();
        final ResolvedType resolvedType = reflectMaid.resolve(genericType);

        assertThat(resolvedType, instanceOf(ClassType.class));
        final ClassType classType = (ClassType) resolvedType;
        final ResolvedType fieldType = classType.fields().get(0).getType();
        assertThat(fieldType.assignableType(), is(List.class));

        final List<ResolvedType> typeParameters = fieldType.typeParameters();
        assertThat(typeParameters.size(), is(1));
        final ResolvedType typeParameter = typeParameters.get(0);
        assertThat(typeParameter.isWildcard(), is(false));
        assertThat(typeParameter.assignableType(), is(Serializable.class));
    }

    @Test
    public void typeToken() {
        final GenericType<List<String>> type = genericType(new TypeToken<>() {
        });
        final ReflectMaid reflectMaid = aReflectMaid();
        assertThat(reflectMaid.resolve(type).simpleDescription(), is("List<String>"));
    }

    private static class InnerClass {
    }
}

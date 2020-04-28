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

import de.quantummaid.reflectmaid.resolver.ResolvedConstructor;
import de.quantummaid.reflectmaid.resolver.ResolvedField;
import de.quantummaid.reflectmaid.resolver.ResolvedMethod;
import de.quantummaid.reflectmaid.resolver.ResolvedParameter;
import de.quantummaid.reflectmaid.types.*;
import de.quantummaid.reflectmaid.unresolved.UnresolvedType;
import de.quantummaid.reflectmaid.validators.NotNullValidator;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static de.quantummaid.reflectmaid.ArrayType.fromArrayClass;
import static de.quantummaid.reflectmaid.ClassType.fromClassWithGenerics;
import static de.quantummaid.reflectmaid.ClassType.fromClassWithoutGenerics;
import static de.quantummaid.reflectmaid.GenericType.fromResolvedType;
import static de.quantummaid.reflectmaid.GenericType.genericType;
import static de.quantummaid.reflectmaid.ResolvedType.resolvedType;
import static de.quantummaid.reflectmaid.TypeResolver.resolveType;
import static de.quantummaid.reflectmaid.TypeVariableName.typeVariableName;
import static de.quantummaid.reflectmaid.WildcardedType.wildcardType;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public final class ReflectMaidSpecs {

    @Test
    public void nullValue() {
        final Exception exception = withException(() -> resolvedType(null));
        assertThat(exception.getMessage(), is("type must not be null"));
    }

    @Test
    public void typeWithoutTypeVariables() {
        final ResolvedType resolvedType = resolvedType(TestType.class);
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
        assertThat(constructor.parameters(), hasSize(0));
        assertThat(constructor.describe(), is("public de.quantummaid.reflectmaid.types.TestType()"));
        assertThat(constructor.constructor().getName(), is("de.quantummaid.reflectmaid.types.TestType"));

        assertThat(classType.methods(), hasSize(1));
        final ResolvedMethod method = classType.methods().get(0);
        assertThat(method.isPublic(), is(true));
        assertThat(method.describe(), is("'String method()' [public java.lang.String de.quantummaid.reflectmaid.types.TestType.method()]"));
        assertThat(method.parameters(), hasSize(0));
        assertThat(method.name(), is("method"));
        assertThat(method.returnType().isPresent(), is(true));
        assertThat(method.method().getName(), is("method"));

        assertThat(classType.fields(), hasSize(1));
        final ResolvedField field = classType.fields().get(0);
        assertThat(field.isPublic(), is(false));
        assertThat(field.isStatic(), is(false));
        assertThat(field.isTransient(), is(false));
        assertThat(field.describe(), is("private String field"));
        assertThat(field.name(), is("field"));
        assertThat(field.type().simpleDescription(), is("String"));
        assertThat(field.field().getName(), is("field"));
    }

    @Test
    public void typeWithTypeVariables() {
        final ResolvedType resolvedType = fromClassWithGenerics(TestTypeWithTypeVariables.class,
                Map.of(typeVariableName("A"), resolvedType(String.class)));

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

        final Exception exception2 = withException(() -> fromClassWithoutGenerics(TestTypeWithTypeVariables.class));
        assertThat(exception2.getMessage(), is("Type variables of 'de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables' cannot be resolved"));

        assertThat(classType.methods(), hasSize(3));
        final ResolvedMethod parameterizedMethod = classType.methods().stream()
                .filter(resolvedMethod -> resolvedMethod.name().equals("foo"))
                .findFirst()
                .orElseThrow();
        assertThat(parameterizedMethod.describe(), is("'void foo(TestTypeWithTypeVariables<String> other)' " +
                "[public void de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables.foo(de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables<A>)]"));
        assertThat(parameterizedMethod.hasParameters(List.of(resolvedType)), is(true));
        assertThat(parameterizedMethod.hasParameters(List.of()), is(false));
        assertThat(parameterizedMethod.hasParameters(List.of(resolvedType(String.class))), is(false));

        assertThat(parameterizedMethod.parameters(), hasSize(1));
        final ResolvedParameter parameter = parameterizedMethod.parameters().get(0);
        assertThat(parameter.name(), is("other"));
        assertThat(parameter.type().simpleDescription(), is("TestTypeWithTypeVariables<String>"));
        assertThat(parameter.parameter().getName(), is("other"));
    }

    @Test
    public void arrayType() {
        final Exception exception1 = withException(() -> fromClassWithoutGenerics(String[].class));
        assertThat(exception1, instanceOf(UnsupportedOperationException.class));

        final Exception exception2 = withException(() -> fromClassWithGenerics(String[].class, Map.of()));
        assertThat(exception2, instanceOf(UnsupportedOperationException.class));

        final Exception exception3 = withException(() -> fromArrayClass(String.class));
        assertThat(exception3, instanceOf(UnsupportedOperationException.class));

        final ResolvedType resolvedType = resolvedType(String[].class);
        assertThat(resolvedType.description(), is("java.lang.String[]"));
        assertThat(resolvedType.simpleDescription(), is("String[]"));
        assertThat(resolvedType.assignableType(), is(String[].class));
        assertThat(resolvedType.isAbstract(), is(false));
        assertThat(resolvedType.isInterface(), is(false));
        assertThat(resolvedType.isWildcard(), is(false));

        assertThat(resolvedType.typeParameters(), hasSize(1));
        final ResolvedType typeParameter = resolvedType.typeParameters().get(0);
        assertThat(typeParameter.simpleDescription(), is("String"));

        assertThat(resolvedType, instanceOf(ArrayType.class));
        final ArrayType arrayType = (ArrayType) resolvedType;
        assertThat(arrayType.componentType().simpleDescription(), is("String"));
    }

    @Test
    public void wildcardedType() {
        final WildcardedType wildcardedType = wildcardType();
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
        final ClassType classType = fromClassWithGenerics(TestTypeWithSpecialParameters.class,
                Map.of(typeVariableName("T"), fromClassWithoutGenerics(String.class)));
        assertThat(classType.methods(), hasSize(1));
        final ResolvedMethod resolvedMethod = classType.methods().get(0);

        assertThat(resolvedMethod.returnType().isPresent(), is(true));
        final ResolvedType returnType = resolvedMethod.returnType().get();
        assertThat(returnType.simpleDescription(), is("List<?>"));

        assertThat(resolvedMethod.parameters(), hasSize(2));
        final ResolvedParameter parameter1 = resolvedMethod.parameters().get(0);
        assertThat(parameter1.type().simpleDescription(), is("String[]"));
        final ResolvedParameter parameter2 = resolvedMethod.parameters().get(1);
        assertThat(parameter2.type().simpleDescription(), is("String[]"));
    }

    @Test
    public void fields() {
        final ClassType classType = fromClassWithoutGenerics(TypeWithFields.class);
        assertThat(classType.fields(), hasSize(2));
        final ResolvedField staticField = classType.fields().stream()
                .filter(resolvedField -> resolvedField.name().equals("FIELD_1"))
                .findFirst()
                .orElseThrow();
        assertThat(staticField.describe(), is("public static final String FIELD_1"));
        assertThat(staticField.isStatic(), is(true));

        final ResolvedField transientField = classType.fields().stream()
                .filter(resolvedField -> resolvedField.name().equals("field2"))
                .findFirst()
                .orElseThrow();
        assertThat(transientField.describe(), is("protected transient String field2"));
        assertThat(transientField.isTransient(), is(true));
        assertThat(transientField.isPublic(), is(false));
    }

    @Test
    public void primitive() {
        final ResolvedType resolvedType = resolvedType(int.class);
        assertThat(resolvedType.description(), is("int"));
        assertThat(resolvedType.simpleDescription(), is("int"));
        assertThat(resolvedType.isAbstract(), is(false));
    }

    @Test
    public void abstractClass() {
        final ResolvedType resolvedType = resolvedType(InputStream.class);
        assertThat(resolvedType.isAbstract(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(false));
    }

    @Test
    public void interfaceAsResolvedType() {
        final ResolvedType resolvedType = resolvedType(Serializable.class);
        assertThat(resolvedType.isInterface(), is(true));
        assertThat(resolvedType.isAbstract(), is(true));
        assertThat(resolvedType.isInstantiatable(), is(false));
    }

    @Test
    public void innerClass() {
        final ResolvedType resolvedType = resolvedType(InnerClass.class);
        assertThat(resolvedType.isInnerClass(), is(true));
        assertThat(resolvedType.description(), is("de.quantummaid.reflectmaid.ReflectMaidSpecs$InnerClass"));
    }

    @Test
    public void methodsWithUnresolvableTypeVariablesAreIgnored() {
        final ClassType classType = fromClassWithoutGenerics(TypeWithUnresolvableTypeVariable.class);
        assertThat(classType.methods(), hasSize(0));
    }

    @Test
    public void syntheticFeaturesAreIgnored() {
        final ClassType classType = fromClassWithoutGenerics(NotNullValidator.class);
        assertThat(classType.fields(), hasSize(0));
        assertThat(classType.constructors(), hasSize(1));
        assertThat(classType.methods(), hasSize(1));
    }

    @Test
    public void unsupportedJvmFeature() {
        final Exception exception = withException(() -> resolveType(new UnsupportedJvmFeature(), fromClassWithoutGenerics(String.class)));
        assertThat(exception.getMessage(), is("Unknown 'Type' implementation by class 'class de.quantummaid.reflectmaid.types.UnsupportedJvmFeature' " +
                "on object 'unsupported'"));
    }

    @Test
    public void unresolvedType() {
        final Exception exception = withException(() -> UnresolvedType.unresolvedType(TestTypeWithTypeVariables.class).resolve());
        assertThat(exception, instanceOf(IllegalArgumentException.class));

        final ResolvedType resolvedType = UnresolvedType.unresolvedType(TestType.class).resolve();
        assertThat(resolvedType.simpleDescription(), is("TestType"));
    }

    @Test
    public void genericTypeWithoutTypeVariables() {
        final GenericType<TestType> genericType = genericType(TestType.class);
        assertThat(genericType.toResolvedType().simpleDescription(), is("TestType"));

        final GenericType<?> fromResolvedType = fromResolvedType(resolvedType(TestType.class));
        assertThat(fromResolvedType.toResolvedType().simpleDescription(), is("TestType"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void genericTypeWithTypeVariables() {
        Exception exception = null;
        try {
            genericType(TestTypeWithTypeVariables.class);
        } catch (final Exception e) {
            exception = e;
        }
        assertThat(exception, notNullValue());
        assertThat(exception.getMessage(), is("type 'de.quantummaid.reflectmaid.types.TestTypeWithTypeVariables' " +
                "contains the following type variables that need to be filled in in order to create a GenericType object: [A]"));

        final GenericType<TestTypeWithTypeVariables> genericType = genericType(TestTypeWithTypeVariables.class, String.class);
        assertThat(genericType.toResolvedType().simpleDescription(), is("TestTypeWithTypeVariables<String>"));
    }

    private static Exception withException(final ExceptionThrowingLambda runnable) {
        Exception exception = null;
        try {
            runnable.run();
        } catch (final Exception e) {
            exception = e;
        }
        assertThat(exception, notNullValue());
        return exception;
    }

    private static class InnerClass {
    }
}

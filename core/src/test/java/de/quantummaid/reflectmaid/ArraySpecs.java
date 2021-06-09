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

import de.quantummaid.reflectmaid.resolvedtype.ArrayType;
import de.quantummaid.reflectmaid.resolvedtype.ResolvedType;
import de.quantummaid.reflectmaid.resolvedtype.resolver.ResolvedField;
import de.quantummaid.reflectmaid.types.TypeWithGenericArray;
import org.junit.jupiter.api.Test;

import static de.quantummaid.reflectmaid.GenericType.genericType;
import static de.quantummaid.reflectmaid.ReflectMaid.aReflectMaid;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public final class ArraySpecs {

    @Test
    public void arrayType() {
        final ReflectMaid reflectMaid = aReflectMaid();

        final ResolvedType resolvedType = reflectMaid.resolve(String[].class);
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
    public void genericArray() {
        final ReflectMaid reflectMaid = aReflectMaid();

        final ResolvedType resolvedType = reflectMaid.resolve(genericType(new TypeToken<TypeWithGenericArray<String>>() {
        }));
        final ResolvedField arrayField = resolvedType.fields().get(0);
        assertThat(arrayField.describe(), is("private final String[] array"));
    }
}

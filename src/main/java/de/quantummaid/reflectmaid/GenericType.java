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

import de.quantummaid.reflectmaid.unresolved.UnresolvedType;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

import static de.quantummaid.reflectmaid.GenericTypeException.genericTypeException;
import static de.quantummaid.reflectmaid.ResolvedType.resolvedType;
import static de.quantummaid.reflectmaid.unresolved.UnresolvedType.unresolvedType;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenericType<T> { // NOSONAR
    private final ResolvedType type;

    public static <T> GenericType<T> genericType(final Class<T> type) {
        final UnresolvedType unresolvedType = unresolvedType(type);
        final List<TypeVariableName> typeVariableNames = unresolvedType.typeVariables();
        if (!typeVariableNames.isEmpty()) {
            final String variables = typeVariableNames.stream()
                    .map(TypeVariableName::name)
                    .collect(joining(", ", "[", "]"));
            throw genericTypeException(format(
                    "type '%s' contains the following type variables that need to be filled in in order to create a %s object: %s",
                    type.getName(),
                    GenericType.class.getSimpleName(),
                    variables
            ));
        }
        final ResolvedType resolvedType = resolvedType(type);
        return new GenericType<>(resolvedType);
    }

    public static <T> GenericType<T> genericType(final Class<T> type, final Class<?>... genericParameters) {
        final GenericType<?>[] genericParameterTypes = stream(genericParameters)
                .map(GenericType::genericType)
                .toArray(GenericType<?>[]::new);
        return genericType(type, genericParameterTypes);
    }

    public static <T> GenericType<T> genericType(final Class<T> type, final GenericType<?>... genericParameters) {
        final UnresolvedType unresolvedType = unresolvedType(type);
        final List<ResolvedType> resolvedParameters = stream(genericParameters)
                .map(GenericType::toResolvedType)
                .collect(toList());
        final ResolvedType resolvedType = unresolvedType.resolve(resolvedParameters);
        return new GenericType<>(resolvedType);
    }

    public static <T> GenericType<T> fromResolvedType(final ResolvedType type) {
        return new GenericType<>(type);
    }

    public ResolvedType toResolvedType() {
        return this.type;
    }
}

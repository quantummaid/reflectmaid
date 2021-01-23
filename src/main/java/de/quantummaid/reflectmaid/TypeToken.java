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

import java.lang.reflect.Type;

import static de.quantummaid.reflectmaid.ClassType.fromClassWithoutGenerics;
import static de.quantummaid.reflectmaid.TypeResolver.resolveType;
import static de.quantummaid.reflectmaid.TypeVariableName.typeVariableName;

@SuppressWarnings({"java:S2326", "java:S1610", "java:S1694"})
public abstract class TypeToken<T> {

    public final ResolvedType toResolvedType() {
        final Class<?> subclass = this.getClass();
        final Type genericSupertype = subclass.getGenericSuperclass();
        final ClassType subclassType = fromClassWithoutGenerics(subclass);
        final ClassType interfaceType = (ClassType) resolveType(genericSupertype, subclassType);
        return interfaceType.typeParameter(typeVariableName("T"));
    }
}

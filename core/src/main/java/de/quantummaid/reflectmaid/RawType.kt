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
package de.quantummaid.reflectmaid

import de.quantummaid.reflectmaid.resolvedtype.Cached
import de.quantummaid.reflectmaid.resolvedtype.NullableCached

data class RawClass internal constructor(private val clazz: Class<*>) {
    private val genericSuperType = NullableCached { clazz.genericSuperclass }
    private val genericInterfaces = Cached { clazz.genericInterfaces }
    private val isArray = Cached { clazz.isArray }
    private val declaredMethods = Cached { clazz.declaredMethods }
    private val declaredConstructors = Cached { clazz.declaredConstructors }
    private val declaredFields = Cached { clazz.declaredFields }
    private val typeParameters = Cached { clazz.typeParameters }
    private val name = Cached { clazz.name }
    private val simpleName = Cached { clazz.simpleName }
    private val modifiers = Cached { clazz.modifiers }
    private val isPrimitive = Cached { clazz.isPrimitive }
    private val isInterface = Cached { clazz.isInterface }
    private val isAnonymousClass = Cached { clazz.isAnonymousClass }
    private val enclosingClass = NullableCached { clazz.enclosingClass }
    private val isLocalClass = Cached { clazz.isLocalClass }
    private val isAnnotation = Cached { clazz.isAnnotation }
    private val componentType = NullableCached { clazz.componentType }

    fun genericSuperType() = genericSuperType.get()
    fun genericInterfaces() = genericInterfaces.get()
    fun isArray() = isArray.get()
    fun declaredMethods() = declaredMethods.get()
    fun declaredConstructors() = declaredConstructors.get()
    fun declaredFields() = declaredFields.get()
    fun typeParameters() = typeParameters.get()
    fun name() = name.get()
    fun simpleName() = simpleName.get()
    fun modifiers() = modifiers.get()
    fun isPrimitive() = isPrimitive.get()
    fun isInterface() = isInterface.get()
    fun isAnonymousClass() = isAnonymousClass.get()
    fun enclosingClass() = enclosingClass.get()
    fun isLocalClass() = isLocalClass.get()
    fun isAnnotation() = isAnnotation.get()
    fun componentType() = componentType.get()

    fun wrappedClass() = clazz
}

class RawClassCache private constructor(private val map: MutableMap<Class<*>, RawClass>) {

    companion object {
        internal fun rawTypeCache() = RawClassCache(LinkedHashMap())
    }

    internal fun rawClassFor(clazz: Class<*>): RawClass {
        return map.computeIfAbsent(clazz) { RawClass(it) }
    }
}
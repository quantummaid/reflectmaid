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
package de.quantummaid.reflectmaid.actors

import kotlin.reflect.KClass

class ActorHasNotBeenClosedYetException : RuntimeException()

fun <State : Any, MessageSuperType : Any> defaultOnCancel(): State.(Actor<State, MessageSuperType>) -> Unit = {
    throw ActorHasNotBeenClosedYetException()
}

class ActorBuilder<State : Any, MessageSuperType : Any>(private val name: String) {

    private lateinit var initialState: State
    private lateinit var pool: ActorPool
    private val mappings: MutableMap<KClass<out MessageSuperType>, MessageHandler<State, MessageSuperType, *>> =
        mutableMapOf()
    private val closeMessages = mutableListOf<KClass<out MessageSuperType>>()
    private var onCancel: State.(Actor<State, MessageSuperType>) -> Unit = defaultOnCancel()

    inline fun <reified ConcreteMessage : MessageSuperType> withMutatingHandler(
        noinline handler: State.(ConcreteMessage) -> Unit
    ): ActorBuilder<State, MessageSuperType> {
        val messageType = ConcreteMessage::class
        return withMutatingHandler(messageType, handler)
    }

    fun <ConcreteMessage : MessageSuperType> withMutatingHandler(
        messageType: KClass<ConcreteMessage>,
        handler: State.(ConcreteMessage) -> Unit
    ): ActorBuilder<State, MessageSuperType> {
        val messageHandler = MessageHandler<State, MessageSuperType, ConcreteMessage> {
            handler(this, it)
            this
        }
        mappings[messageType] = messageHandler
        return this
    }

    fun withInitialState(initialState: State): ActorBuilder<State, MessageSuperType> {
        this.initialState = initialState
        return this
    }

    fun withPool(pool: ActorPool): ActorBuilder<State, MessageSuperType> {
        this.pool = pool
        return this
    }

    fun launch(): Actor<State, MessageSuperType> {
        val handlers = MessageHandlers(mappings)
        return Actor.launch(name, pool, initialState, handlers, closeMessages, onCancel)
    }

    inline fun <reified ConcreteMessage : MessageSuperType> closeOn(): ActorBuilder<State, MessageSuperType> {
        return closeOn<ConcreteMessage> {}
    }

    inline fun <reified ConcreteMessage : MessageSuperType> closeOn(
        noinline handler: State.(ConcreteMessage) -> Unit
    ): ActorBuilder<State, MessageSuperType> {
        return closeOn(ConcreteMessage::class, handler)
    }

    fun <ConcreteMessage : MessageSuperType> closeOn(
        messageType: KClass<ConcreteMessage>,
        handler: State.(ConcreteMessage) -> Unit
    ): ActorBuilder<State, MessageSuperType> {
        val messageHandler = MessageHandler<State, MessageSuperType, ConcreteMessage> {
            handler(this, it)
            this
        }
        mappings[messageType] = messageHandler
        closeMessages.add(messageType)
        return this
    }

    fun onCancel(onCancel: State.(Actor<State, MessageSuperType>) -> Unit): ActorBuilder<State, MessageSuperType> {
        this.onCancel = onCancel
        return this
    }
}

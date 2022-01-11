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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ActorMessage<Message : Any>(
    val delegate: Message,
    val exception: CompletableDeferred<Throwable?> = CompletableDeferred()
)

class Actor<State : Any, Message : Any> private constructor(
    val name: String,
    val channel: Channel<ActorMessage<Message>>,
    private var currentState: State,
    private val messageHandlers: MessageHandlers<State, Message>,
    private val closeMessages: List<KClass<out Message>>,
    private val onCancel: State.(Actor<State, Message>) -> Unit
) : AutoCloseable {
    lateinit var job: Job

    companion object {
        fun <State : Any, Message : Any> launch(
            name: String,
            pool: ActorPool,
            initialState: State,
            msgHandlers: MessageHandlers<State, Message>,
            closeMessages: List<KClass<out Message>>,
            onCancel: State.(Actor<State, Message>) -> Unit
        ): Actor<State, Message> {
            require(closeMessages.isNotEmpty()) { "at least one close message type must be provided so that the actor is closable" }
            val channel: Channel<ActorMessage<Message>> = Channel()
            val actor = Actor(name, channel, initialState, msgHandlers, closeMessages, onCancel)
            val job = pool.launch(actor)
            actor.job = job
            return actor
        }
    }

    internal suspend fun handleMessagesOnChannel() {
        for (actorMessage in channel) {
            val closing = closeMessages.any { it.isInstance(actorMessage.delegate) }
            if (closing) {
                channel.close()
            }
            val currentMessage = actorMessage.delegate
            try {
                currentState = messageHandlers.handle(currentState, currentMessage)
                actorMessage.exception.complete(null)
            } catch (t: Throwable) {
                val exceptionWithInfo = ActorMessageHandlingException(name, currentMessage)
                t.addSuppressed(exceptionWithInfo)
                actorMessage.exception.complete(t)
            }
            if (closing) {
                return
            }
        }
    }

    fun signalAwaitingSuccess(msg: Message, timeout: Duration = milliseconds(100)) {
        val coroutineName = CoroutineName("$name!$timeout($msg}")
        val exception = runBlocking(coroutineName) {
            try {
                withTimeout(timeout) {
                    val actorMessage = ActorMessage(msg)
                    channel.send(actorMessage)
                    actorMessage.exception.await()
                }
            } catch (e: TimeoutCancellationException) {
                throw UnsupportedOperationException("signalAwaitingSuccess timeout after $timeout (msg:$msg)", e)
            }
        }
        if (exception != null) {
            throw exception
        }
    }

    fun isActive(): Boolean {
        return job.isActive
    }

    fun onCancel() {
        currentState.onCancel(this)
    }

    override fun close() {
        channel.close()
    }

    override fun toString(): String {
        return "Actor(name='$name', " +
                "job=$job, " +
                "job.isActive=${job.isActive}, " +
                "job.isCompleted=${job.isCompleted}, " +
                "job.isCancelled=${job.isCancelled}, " +
                "channel=${channel}, " +
                "channel.isClosedForReceive=${channel.isClosedForReceive}, " +
                "channel.isClosedForSend=${channel.isClosedForSend}, " +
                "currentState=$currentState)"
    }
}

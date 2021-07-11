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

import kotlinx.coroutines.*
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentLinkedQueue

open class ActorPool(executorCoroutineDispatcher: CoroutineDispatcher = Dispatchers.Default) : AutoCloseable {
    companion object {
        fun fixedThreadPoolDispatcher(numberOfThreads: Int, name: String): ExecutorCoroutineDispatcher {
            return newFixedThreadPoolContext(numberOfThreads, name)
        }
    }

    private val scope = CoroutineScope(executorCoroutineDispatcher + SupervisorJob())
    val activeActors = ConcurrentLinkedQueue<Actor<*, *>>()

    fun launch(actor: Actor<*, *>): Job {
        synchronized(this) {
            if (!scope.isActive) {
                throw ActorPoolAlreadyClosedException()
            }
            val deferredActorReference = CompletableDeferred<Actor<*, *>>()
            val job: Job = scope.launch {
                val deferredActor = withTimeout(100) {
                    val await = deferredActorReference.await()
                    await
                }
                var cancellationException: CancellationException? = null
                try {
                    actor.handleMessagesOnChannel()
                } catch (e: CancellationException) {
                    cancellationException = e
                }
                activeActors.remove(deferredActor)
                if (cancellationException != null) {
                    throw cancellationException
                }
            }

            activeActors.add(actor)
            deferredActorReference.complete(actor)
            return job
        }
    }

    override fun close() {
        synchronized(this) {
            val exceptionsDuringCancel = mutableMapOf<String, Throwable>()
            activeActors.toList()
                .filter { !it.channel.isClosedForReceive }
                .forEach {
                    try {
                        it.onCancel()
                    } catch (e: Throwable) {
                        exceptionsDuringCancel[it.name] = e
                    }
                }
            scope.cancel()
            val timeout = 2_000
            val endTime = currentTimeMillis() + timeout
            while (currentTimeMillis() < endTime) {
                if (activeActors.isEmpty()) {
                    if (exceptionsDuringCancel.isNotEmpty()) {
                        val exception =
                            ExceptionsDuringCancelOfActors(actorOnCancelExceptionsToString(exceptionsDuringCancel))
                        exceptionsDuringCancel.values.forEach { exception.addSuppressed(it) }
                        throw exception
                    }
                    return
                }
                sleep(10)
            }
            val exception =
                TimeoutDuringCloseOfActorPoolException(
                    "active actors: ${activeActors.joinToString { it.name }};" +
                            " ${actorOnCancelExceptionsToString(exceptionsDuringCancel)}"
                )
            exceptionsDuringCancel.values.forEach { exception.addSuppressed(it) }
            throw exception
        }
    }
}

private fun actorOnCancelExceptionsToString(exceptionsDuringCancel: MutableMap<String, Throwable>): String {
    return if (exceptionsDuringCancel.isEmpty()) {
        ""
    } else {
        "exceptions during cancel: " + exceptionsDuringCancel
            .map { (name, throwable) -> "$name: ${throwable.javaClass.simpleName}: ${throwable.message}" }
            .joinToString()
    }
}

class ActorPoolAlreadyClosedException : RuntimeException()
class ExceptionsDuringCancelOfActors(override val message: String) : RuntimeException()
class TimeoutDuringCloseOfActorPoolException(override val message: String) : RuntimeException()
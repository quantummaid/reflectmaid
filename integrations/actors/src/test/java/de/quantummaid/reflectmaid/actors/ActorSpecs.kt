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

import de.quantummaid.reflectmaid.actors.ActorPool.Companion.fixedThreadPoolDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.time.seconds

sealed class MyMessage

object IncrementMessage : MyMessage()
object DecrementMessage : MyMessage()
data class ThrowExceptionMessage(val message: String) : MyMessage()
object NonRegisteredMessage : MyMessage()
data class SleepMessage(val sleepTimeInMilliseconds: Long) : MyMessage()
data class CloseMessage(val count: CompletableDeferred<Int> = CompletableDeferred()) : MyMessage()

class MyState {
    @Volatile
    var count = 0

    fun increment() {
        count++
    }

    fun decrement() {
        count--
    }
}

class ActorSpecs {

    @Test
    fun emptyActorPoolCanBeClosed() {
        val actorPool = ActorPool(fixedThreadPoolDispatcher(1, "foo"))
        assertEquals(0, actorPool.activeActors.size)
        actorPool.close()
    }

    @Test
    fun actorsCanDefineEndStateWhichCanExportData() {
        val actorPool = ActorPool()

        assertEquals(0, actorPool.activeActors.size)

        val state = MyState()
        val actor = ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(state)
            .withMutatingHandler<IncrementMessage> { increment() }
            .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
            .closeOn<CloseMessage> {
                it.count.complete(count)
            }
            .launch()
        assertEquals(1, actorPool.activeActors.size)

        actor.signalAwaitingSuccess(IncrementMessage)

        val closeMessage = CloseMessage()
        actor.signalAwaitingSuccess(closeMessage)
        waitForActorPoolToBecomeEmpty(actorPool)

        val count = runBlocking { closeMessage.count.await() }
        assertEquals(1, count)
    }

    @Test
    fun closingAnActorPoolWithActiveActorsCausesAnException() {
        val actorPool = ActorPool()

        assertEquals(0, actorPool.activeActors.size)

        val state = MyState()
        ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(state)
            .withMutatingHandler<IncrementMessage> { increment() }
            .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
            .closeOn<CloseMessage> {
                it.count.complete(count)
            }
            .launch()
        assertEquals(1, actorPool.activeActors.size)

        var exception: ExceptionsDuringCancelOfActors? = null
        try {
            actorPool.close()
        } catch (e: ExceptionsDuringCancelOfActors) {
            exception = e
        }
        assertNotNull(exception)
        assertEquals("exceptions during cancel: myactor: ActorHasNotBeenClosedYetException: null", exception!!.message)
    }

    @Test
    fun actorsCanOverwriteOnCancelBehaviour() {
        val actorPool = ActorPool()

        assertEquals(0, actorPool.activeActors.size)

        val state = MyState()
        ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(state)
            .withMutatingHandler<IncrementMessage> { increment() }
            .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
            .closeOn<CloseMessage> {
                it.count.complete(count)
            }
            .onCancel {
            }
            .launch()
        assertEquals(1, actorPool.activeActors.size)

        var exception: TimeoutDuringCloseOfActorPoolException? = null
        try {
            actorPool.close()
        } catch (e: TimeoutDuringCloseOfActorPoolException) {
            exception = e
        }
        assertNotNull(exception)
        assertEquals("active actors: myactor; ", exception!!.message)
    }

    @Test
    fun actorsInClosedActorPoolCannotReceiveMessages() {
        val actorPool = ActorPool()

        assertEquals(0, actorPool.activeActors.size)

        val state = MyState()
        val actor = ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(state)
            .withMutatingHandler<IncrementMessage> { increment() }
            .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
            .closeOn<CloseMessage>()
            .launch()

        assertTrue(actor.isActive())
        assertEquals(1, actorPool.activeActors.size)

        actor.signalAwaitingSuccess(IncrementMessage)
        actor.signalAwaitingSuccess(CloseMessage())

        actorPool.close()
        println("done closing pool")
        var exception: ClosedSendChannelException? = null
        try {
            actor.signalAwaitingSuccess(SleepMessage(10_000), 20.seconds)
        } catch (e: ClosedSendChannelException) {
            exception = e
        }
        assertNotNull(exception)
        assertEquals("Channel was closed", exception!!.message)

        waitForActorPoolToBecomeEmpty(actorPool)

        actor.close()
    }

    @Test
    fun actorsCannotBeCreatedInClosedActorPool() {
        val actorPool = ActorPool(fixedThreadPoolDispatcher(1, "foo"))
        assertEquals(0, actorPool.activeActors.size)
        actorPool.close()


        var exception: ActorPoolAlreadyClosedException? = null

        try {
            val state = MyState()
            ActorBuilder<MyState, MyMessage>("myactor")
                .withPool(actorPool)
                .withInitialState(state)
                .withMutatingHandler<IncrementMessage> { increment() }
                .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
                .closeOn<CloseMessage>()
                .launch()
        } catch (e: ActorPoolAlreadyClosedException) {
            exception = e
        }
        assertNotNull(exception)
    }

    @Disabled
    @Test
    fun oneActorCanWaitForAnotherActorEvenIfTheActorPoolHasOnlyOneThread() {
        val actorPool = ActorPool(fixedThreadPoolDispatcher(1, "foo"))
        assertEquals(0, actorPool.activeActors.size)

        val initialState = MyState()
        val actor = ActorBuilder<MyState, MyMessage>("myactor0")
            .withPool(actorPool)
            .withInitialState(initialState)
            .withMutatingHandler<IncrementMessage> { increment() }
            .closeOn<CloseMessage>()
            .launch()

        val delegatingActor = ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(MyState())
            .withMutatingHandler<IncrementMessage> { actor.signalAwaitingSuccess(it, 10.seconds) }
            .closeOn<CloseMessage>()
            .launch()

        delegatingActor.signalAwaitingSuccess(IncrementMessage, 10.seconds)
    }

    @Test
    fun testActor() {
        val actorPool = ActorPool(fixedThreadPoolDispatcher(1, "foo"))

        assertEquals(0, actorPool.activeActors.size)

        val state = MyState()
        val actor = ActorBuilder<MyState, MyMessage>("myactor")
            .withPool(actorPool)
            .withInitialState(state)
            .withMutatingHandler<IncrementMessage> { increment() }
            .withMutatingHandler<DecrementMessage> { decrement() }
            .withMutatingHandler<ThrowExceptionMessage> { throw UnsupportedOperationException(it.message) }
            .withMutatingHandler<SleepMessage> { sleep(it.sleepTimeInMilliseconds) }
            .closeOn<CloseMessage>()
            .launch()

        assertTrue(actor.isActive())
        assertEquals(1, actorPool.activeActors.size)

        assertStateUpdateWorks(state, actor)
        assertExceptionsAreRethrown(actor)
        assertUnkownMessageCausesException(actor)
        assertDefaultTimeoutWorks(actor)
        assertTimeoutCanBeSpecified(actor)

        actor.close()
        waitForActorPoolToBecomeEmpty(actorPool)

        assertEquals(0, actorPool.activeActors.size)
    }

    private fun assertStateUpdateWorks(state: MyState, actor: Actor<MyState, MyMessage>) {
        assertEquals(0, state.count)

        actor.signalAwaitingSuccess(IncrementMessage)
        assertEquals(1, state.count)

        actor.signalAwaitingSuccess(DecrementMessage)
        assertEquals(0, state.count)
    }

    private fun assertExceptionsAreRethrown(actor: Actor<MyState, MyMessage>) {
        var exception: UnsupportedOperationException? = null
        try {
            actor.signalAwaitingSuccess(ThrowExceptionMessage("my exception"))
        } catch (e: UnsupportedOperationException) {
            exception = e
        }
        assertNotNull(exception)
        assertEquals("my exception", exception!!.message)
    }

    private fun assertUnkownMessageCausesException(actor: Actor<MyState, MyMessage>) {
        var exception: UnsupportedMessageException? = null
        try {
            actor.signalAwaitingSuccess(NonRegisteredMessage)
        } catch (e: UnsupportedMessageException) {
            exception = e
        }
        assertNotNull(exception)
    }

    private fun assertDefaultTimeoutWorks(actor: Actor<MyState, MyMessage>) {
        var exception: TimeoutCancellationException? = null
        try {
            actor.signalAwaitingSuccess(SleepMessage(1000))
        } catch (e: TimeoutCancellationException) {
            exception = e
        }
        assertNotNull(exception)
    }

    private fun assertTimeoutCanBeSpecified(actor: Actor<MyState, MyMessage>) {
        actor.signalAwaitingSuccess(SleepMessage(1000), 2.seconds)
    }

    private fun waitForActorPoolToBecomeEmpty(actorPool: ActorPool) {
        runBlocking {
            val timeout = System.currentTimeMillis() + 10_000
            while (actorPool.activeActors.size > 0 && System.currentTimeMillis() < timeout) {
                delay(1)
            }
            assertEquals(0, actorPool.activeActors.size)
        }
    }
}

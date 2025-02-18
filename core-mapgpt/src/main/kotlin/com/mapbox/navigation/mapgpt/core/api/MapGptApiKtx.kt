package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.transform
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Extension that simplifies collection of conversation responding events
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun MapGptService.conversationChunks(): Flow<SessionFrame.SendEvent.Body.Conversation> = conversationStatus
    .filterIsInstance<ConversationState.Responding>()
    .flatMapLatest { it.bufferedConversation.chunks }

/**
 * Extension that simplifies collection of events for the currently active conversation.
 *
 * An active conversation is defined as a chain of uninterrupted [ConversationState.Responding] states.
 * There can be multiple, different [ConversationState.Responding] states delivered without
 * a [ConversationState.Idle] state in-between when unprompted conversation are being queued.
 *
 * Events can be delivered even after all the chunks within an active conversation chain are delivered
 * and the [ConversationState] becomes [ConversationState.Idle] (and conversations are moved to [MapGptServiceImpl.interactionHistory]).
 *
 * This extension will keep observing the events until a new user input request is sent
 * or a chain is broken up by an [ConversationState.Idle] event.
 *
 * TODO: the interruption by IDLE event is not implemented yet, only new [ConversationState.ProcessingInput] breaks the chain right now
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T : SessionFrame.SendEvent.Body> MapGptService.activeConversationEvents(
    type: KClass<T>,
): Flow<T> {
    return conversationStatus
        .flatMapLatest { conversationState ->
            when (conversationState) {
                is ConversationState.Idle -> {
                    interactionHistory.flatMapLatest { history ->
                        val eventsFromUninterruptedOutput = history.takeLastWhile {
                            it is InteractionHistoryElement.Output
                        }.map {
                            (it as InteractionHistoryElement.Output).historyOutput.bufferedOutput.events
                        }
                        if (eventsFromUninterruptedOutput.isEmpty()) {
                            emptyFlow()
                        } else {
                            combine(eventsFromUninterruptedOutput) { values ->
                                values.flatMap {
                                    it
                                }
                            }
                        }
                    }
                }
                is ConversationState.Responding -> {
                    conversationState.bufferedConversation.events
                }
                is ConversationState.NoResponse -> {
                    conversationState.bufferedNoResponse.events
                }
                else -> {
                    emptyFlow()
                }
            }
        }
        .scan(
            initial = emptyList<SessionFrame.SendEvent.Body>()
                to emptyList<SessionFrame.SendEvent.Body>(),
        ) { (_, prevEvents), events ->
            prevEvents to events
        }
        .transform { (prevEvents, events) ->
            events.filter { event ->
                prevEvents.none { prevEvent ->
                    event.id == prevEvent.id
                }
            }.forEach {
                emit(it)
            }
        }
        .mapNotNull { type.safeCast(it) }
        .distinctUntilChanged()
}

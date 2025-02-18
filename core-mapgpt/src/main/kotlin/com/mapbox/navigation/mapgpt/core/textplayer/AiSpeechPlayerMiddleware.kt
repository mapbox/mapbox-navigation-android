package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import com.mapbox.navigation.mapgpt.core.api.ConversationState
import com.mapbox.navigation.mapgpt.core.api.SessionFrame
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.userinput.UserInputState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * Manages the AI conversation with the TTS player. When attached, it is enabled and functional.
 * There are no callers for this middleware, as it takes internal state and manages the TTS player.
 *
 * This will ensure the TTS player audio is muted when the user is speaking and will play the AI responses.
 * The state is surfaced through [AiSpeechPlayer.isSpeakingConfirmation].
 */
class AiSpeechPlayerMiddleware :
    AiSpeechPlayer,
    CoroutineMiddleware<AiSpeechPlayerMiddleware.Context>() {

    class Context(
        val player: Player,
        val userInputState: StateFlow<UserInputState>,
        val conversationState: StateFlow<ConversationState>,
        val voiceState: StateFlow<Voice>,
    ): MiddlewareContext

    private val _aiSpeechChunks = mutableMapOf<String, AiSpeechChunk>()
    private val _isSpeakingConfirmation = MutableStateFlow(false)
    override val isSpeakingConfirmation: StateFlow<Boolean> = _isSpeakingConfirmation

    override fun onAttached(middlewareContext: Context) {
        super.onAttached(middlewareContext)
        SharedLog.i(TAG) { "onAttached" }

        middlewareContext.player.state.onEach(::onPlayerStateChange).launchIn(mainScope)

        middlewareContext.launchMuteWhenListening()
        middlewareContext.launchAiSpeechPlayer()
    }

    override fun onDetached(middlewareContext: Context) {
        super.onDetached(middlewareContext)
        _aiSpeechChunks.clear()
        _isSpeakingConfirmation.value = false
        SharedLog.i(TAG) { "onDetached" }
    }

    private fun Context.launchMuteWhenListening() {
        userInputState.map { it is UserInputState.Listening }
            .distinctUntilChanged()
            .onEach { isListening -> player.isMuted = isListening }
            .launchIn(mainScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Context.launchAiSpeechPlayer() {
        conversationState
            .combine(userInputState) { conversationState, inputState ->
                conversationState.takeIf { inputState is UserInputState.Result }
            }
            .filterIsInstance<ConversationState.Responding>()
            .distinctUntilChanged()
            .flatMapLatest { it.bufferedConversation.chunks }
            .mapNotNull { chunk ->
                chunk.data.content.ifBlank { null }?.let { text ->
                    AiSpeechChunk(
                        chunk = chunk,
                        announcement = Announcement.Regular(
                            text = text,
                            voice = voiceState.value,
                        ),
                        playerState = null,
                    )
                }
            }
            .onEach { chunk ->
                SharedLog.d(TAG) { "Play chunk as regular announcement: $chunk" }
                _aiSpeechChunks[chunk.announcement.utteranceId] = chunk
                player.play(chunk.announcement)
            }
            .launchIn(mainScope)
    }

    private fun onPlayerStateChange(playerState: PlayerState) {
        fun updatePlayerState(utteranceId: String) {
            _aiSpeechChunks[utteranceId]?.let { aiSpeechChunk ->
                _aiSpeechChunks[utteranceId] = aiSpeechChunk.copy(playerState = playerState)
            }
        }
        SharedLog.d(TAG) { "onPlayerStateChange: $playerState" }
        when (playerState) {
            is PlayerState.Preparing -> updatePlayerState(playerState.utteranceId)
            is PlayerState.Speaking -> updatePlayerState(playerState.utteranceId)
            is PlayerState.Stopped -> updatePlayerState(playerState.utteranceId)
            is PlayerState.Done -> {
                _aiSpeechChunks.remove(playerState.utteranceId)
            }
            is PlayerState.Error -> {
                playerState.utteranceId?.let { _aiSpeechChunks.remove(it) }
            }
            PlayerState.Idle -> {
                _aiSpeechChunks.clear()
            }
        }
        onAiSpeechChunkChange(_aiSpeechChunks.toMutableMap())
    }

    private fun onAiSpeechChunkChange(aiSpeechChunks: Map<String, AiSpeechChunk>) {
        val isSpeakingConfirmation = aiSpeechChunks.values.any { aiSpeakingChunk ->
            val isSpeaking = aiSpeakingChunk.playerState is PlayerState.Speaking
            isSpeaking && aiSpeakingChunk.chunk?.data?.confirmation == true
        }
        _isSpeakingConfirmation.value = isSpeakingConfirmation
    }

    private data class AiSpeechChunk(
        val chunk: SessionFrame.SendEvent.Body.Conversation?,
        val announcement: Announcement,
        val playerState: PlayerState?,
    )

    private companion object {
        private const val TAG = "AiSpeechPlayerMiddleware"
    }
}

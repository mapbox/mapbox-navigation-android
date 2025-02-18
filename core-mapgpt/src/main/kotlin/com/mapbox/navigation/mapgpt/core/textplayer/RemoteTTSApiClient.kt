package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import io.ktor.utils.io.ByteReadChannel

interface RemoteTTSApiClient {
    @RemoteTtsProvider
    val provider: String
    fun availableLanguages(): Set<Language>
    fun availableVoices(): Set<Voice>
    suspend fun requestAudioBytes(text: String): Result<ByteReadChannel>
}

internal object NoRemoteTTSApiClient : RemoteTTSApiClient {
    override val provider: String = RemoteTtsProvider.DISABLED
    override fun availableLanguages(): Set<Language> = emptySet()
    override fun availableVoices(): Set<Voice> = emptySet()
    override suspend fun requestAudioBytes(text: String): Result<ByteReadChannel> =
        Result.failure(TtsApiException("No remote TTS API client available"))
}

class TtsApiException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

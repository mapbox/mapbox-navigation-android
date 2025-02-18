package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.appendPathSegments
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel

internal class CoreVoiceApiClient(
    private val httpClient: HttpClient,
    private val languageRepository: LanguageRepository,
    private val voiceOptions: MapGptVoicePlayerOptions,
) : RemoteTTSApiClient {
    @RemoteTtsProvider
    override val provider: String = RemoteTtsProvider.CORE_VOICE

    override fun availableLanguages(): Set<Language> = availableLanguages

    override fun availableVoices(): Set<Voice> {
        return setOf(CoreVoice.Female, CoreVoice.Male)
    }

    override suspend fun requestAudioBytes(text: String): Result<ByteReadChannel> {
        // TODO add ssml https://mapbox.atlassian.net/browse/NAVAND-4037
        val textType = "text"
        val languageTag = languageRepository.language.value.languageTag
        val voice = (languageRepository.voice.value as? CoreVoice)
        val accessToken = voiceOptions.accessToken

        SharedLog.d(TAG) { "Requesting audio for text=$text, voice=$voice" }
        return runCatching {
            val urlBuilder = URLBuilder().apply {
                protocol = URLProtocol.HTTPS
                host = "api.mapbox.com"
                appendPathSegments("voice", "v1", "speak")
                appendEncodedPathSegments(text.encodeURLPath())
                parameters.append("textType", textType)
                parameters.append("outputFormat", "mp3")
                parameters.append("language", languageTag)
                parameters.append("access_token", accessToken)
                voice?.gender?.let { parameters.append("gender", it) }
            }
            SharedLog.logObfuscated(urlBuilder)
            val response: HttpResponse = httpClient.get(urlBuilder.build())
            SharedLog.d(TAG) { "HttpResponse status: ${response.status}" }

            if (response.status.isSuccess()) {
                SharedLog.d(TAG) {
                    "contentType ${response.contentType()}, " +
                    "contentLength ${response.contentLength()}"
                }
                response.bodyAsChannel()
            } else {
                val errorBody = response.bodyAsText()
                throw TtsApiException("Error: ${response.status.description}. Details: $errorBody")
            }
        }
    }

    private fun SharedLog.logObfuscated(builder: URLBuilder) = i(com.mapbox.navigation.mapgpt.core.textplayer.CoreVoiceApiClient.TAG) {
        val obfuscated = builder.buildString().replace(
            regex = Regex("access_token=[^&]+"),
            replacement = "access_token=***",
        )
        "Requesting audio from: $obfuscated"
    }

    sealed class CoreVoice(
        val gender: String,
    ) : Voice {
        object Female : CoreVoice(gender = "female")
        object Male : CoreVoice(gender = "male")

        override fun toString(): String = "CoreVoice(gender=$gender)"
    }

    private companion object {
        private const val TAG = "CoreVoiceApiClient"

        /**
         * Default available languages.
         * https://docs.mapbox.com/api/navigation/directions/#instructions-languages
         */
        val availableLanguages by lazy {
            setOf(
                // English
                "en-US",
                // Spanish
                "es-ES",
                // French
                "fr-FR",
                // German
                "de-DE",
                // Dutch
                "nl-NL",
                // Italian
                "it-IT",
                // Japanese
                "ja-JP",
                // Korean
                "ko-KR",
                // Chinese
                "zh-CN",
            ).map { Language(it) }.toSet()
        }
    }
}

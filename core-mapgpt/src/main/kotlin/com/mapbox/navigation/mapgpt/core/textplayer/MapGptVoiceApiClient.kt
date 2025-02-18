package com.mapbox.navigation.mapgpt.core.textplayer

import com.mapbox.navigation.mapgpt.core.api.MapGptHttpClient
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.Json

internal class MapGptVoiceApiClient internal constructor(
    private val httpClient: MapGptHttpClient,
    private val languageRepository: LanguageRepository,
    private val voiceOptions: MapGptVoicePlayerOptions,
) : RemoteTTSApiClient {
    @RemoteTtsProvider
    override val provider: String = RemoteTtsProvider.MAPGPT_VOICE

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // This can be converted into a call to the remote api. Our current solution has a hardcoded
    // list of available languages.
    override fun availableLanguages(): Set<Language> = availableLanguages
    override fun availableVoices(): Set<DashVoice> = voiceOptions.voices

    @Suppress("MagicNumber")
    override suspend fun requestAudioBytes(
        text: String,
    ): Result<ByteReadChannel> {
        SharedLog.i(TAG) { "producing audio bytes for '$text'" }
        val modelId = languageRepository.language.value.let { language ->
            when (language) {
                Language("en") -> MODEL_ID_TURBO
                else -> MODEL_ID_MULTILINGUAL
            }
        }
        val voiceRequest = DashVoiceRequest(
            text = text,
            modelId = modelId,
            language = languageRepository.language.value.languageTag,
        )
        val requestBody = json.encodeToJsonElement(
            DashVoiceRequest.serializer(),
            voiceRequest,
        ).toString()
        val repositoryVoice = languageRepository.voice.value
        val selectedVoice = repositoryVoice as? DashVoice ?: run {
            SharedLog.e(TAG) { "Selected voice is not a DashVoice: $repositoryVoice" }
            availableVoices().first()
        }

        val voiceId = selectedVoice.personaVoiceId
        val formattedPath = "v1/text-to-speech/$voiceId/stream"
        val queries = mapOf(
            ACCESS_TOKEN_QUERY_KEY to voiceOptions.accessToken,
            OPTIMIZE_LATENCY_QUERY_KEY to LATENCY_QUERY_VALUE,
        )

        SharedLog.i(TAG) { "Request URL: $formattedPath" }
        SharedLog.i(TAG) { "Request body: $requestBody" }
        SharedLog.logObfuscated(queries)

        return runCatching {
            val httpStatement = httpClient.preparePost(
                apiHost = HOST,
                path = formattedPath,
                jsonBody = requestBody,
                queries = queries,
            )
            val response = httpStatement.execute()
            SharedLog.d(TAG) { "HttpResponse status: ${response.status}" }
            if (!response.status.isSuccess()) {
                throw TtsApiException("Failed to request audio bytes: ${response.status}")
            }
            response.bodyAsChannel()
        }
    }

    private fun SharedLog.logObfuscated(queries: Map<String, String>) = i(com.mapbox.navigation.mapgpt.core.textplayer.MapGptVoiceApiClient.TAG) {
        val obfuscatedQueries = queries.map {
            if (it.key == com.mapbox.navigation.mapgpt.core.textplayer.MapGptVoiceApiClient.ACCESS_TOKEN_QUERY_KEY) {
                "${it.key}: ***${it.value.takeLast(4)}"
            } else {
                "${it.key}: ${it.value}"
            }
        }
        "Request queries: $obfuscatedQueries"
    }

    internal companion object {
        private const val TAG = "DashVoiceApiClient"
        private const val HOST = "api-navgptvoice-production.mapbox.com"
        private const val ACCESS_TOKEN_QUERY_KEY = "access_token"
        private const val OPTIMIZE_LATENCY_QUERY_KEY = "optimize_streaming_latency"
        private const val MODEL_ID_TURBO = "eleven_turbo_v2"
        private const val MODEL_ID_MULTILINGUAL = "eleven_multilingual_v2"

        /**
         * The latency value to be used with the [OPTIMIZE_LATENCY_QUERY_KEY] query.
         *
         * https://help.elevenlabs.io/hc/en-us/articles/15726761640721-Can-I-reduce-API-latency-
         */
        private const val LATENCY_QUERY_VALUE = "4"

        /**
         * Default available languages.
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

package com.mapbox.navigation.mapgpt.core.textplayer

import android.content.Context
import com.mapbox.navigation.mapgpt.core.PlatformContext
import com.mapbox.navigation.mapgpt.core.audiofocus.MapGptAudioFocusManager
import com.mapbox.navigation.mapgpt.core.config.api.RemoteTtsProvider
import com.mapbox.navigation.mapgpt.core.analytics.MapGptAnalytics
import com.mapbox.navigation.mapgpt.core.analytics.NoMapGptAnalytics
import com.mapbox.navigation.mapgpt.core.api.MapGptHttpStreamClientImpl
import com.mapbox.navigation.mapgpt.core.common.SharedLog
import com.mapbox.navigation.mapgpt.core.language.LanguageRepository
import com.mapbox.navigation.mapgpt.core.reachability.SharedReachability
import com.mapbox.navigation.mapgpt.core.textplayer.middleware.VoicePlayerMiddlewareContext
import com.mapbox.navigation.mapgpt.core.textplayer.options.PlayerOptions
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * Factory for instantiating the services required for the playing voice announcements.
 *
 * @param context The application [Context].
 * @param languageRepository The repository that provides the selected language and voice.
 * @param sharedReachability The reachability service for detecting network conditions.
 * @param mapGptVoicePlayerOptions The options for the MapGpt voice player.
 * @param playerOptions The android specific player options.
 * @param mapGptAnalytics The analytics instance to use.
 * @param ttsEngine The TTS engine to use. If null, the default engine will be used.
 */
class PlatformAudioPlayerFactory(
    private val context: Context,
    private val languageRepository: LanguageRepository,
    private val sharedReachability: SharedReachability,
    private val mapGptVoicePlayerOptions: MapGptVoicePlayerOptions,
    private val playerOptions: PlayerOptions = PlayerOptions.Builder().build(),
    private val mapGptAnalytics: MapGptAnalytics = NoMapGptAnalytics(),
    private val ttsEngine: String? = null,
) {

    private val attributes: PlayerAttributes by lazy {
        VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(playerOptions)
    }
    private val audioFocusManager by lazy {
        MapGptAudioFocusManager(context, playerOptions)
    }
    private val coreHttpClient by lazy {
        HttpClient(CIO)
    }

    private val speechFileManager by lazy {
        SpeechFileManagerImpl(context.cacheDir)
    }

    private val speechFilePlayer by lazy {
        SpeechFilePlayerImpl(speechFileManager, attributes)
    }

    fun createContext(): VoicePlayerMiddlewareContext {
        return VoicePlayerMiddlewareContext(
            PlatformContext(context = context),
            languageRepository.language,
            languageRepository.voice,
            audioFocusManager,
            createSoundPlayer(context, playerOptions),
        )
    }

    fun createLocalTTSPlayer(): LocalTTSPlayer {
        return MapGptLocalTTSPlayer(
            context,
            languageRepository,
            attributes,
            ttsEngine,
        )
    }

    fun createRemoteTTSPlayer(@RemoteTtsProvider remoteTtsProvider: String): RemoteTTSPlayer {
        val apiClient = when (remoteTtsProvider) {
            RemoteTtsProvider.MAPGPT_VOICE -> createMapGptVoiceClient()
            RemoteTtsProvider.CORE_VOICE -> createCoreVoiceClient()
            RemoteTtsProvider.DISABLED -> NoRemoteTTSApiClient
            else -> {
                SharedLog.w(TAG) { "Unknown provider: $remoteTtsProvider, remote TTS is disabled" }
                NoRemoteTTSApiClient
            }
        }
        return RemoteTTSPlayerImpl(
            remoteTTSApiClient = apiClient,
            speechFileManager = speechFileManager,
            speechFilePlayer = speechFilePlayer,
            sharedReachability = sharedReachability,
        )
    }


    private fun createMapGptVoiceClient(): RemoteTTSApiClient {
        return MapGptVoiceApiClient(
            MapGptHttpStreamClientImpl(),
            languageRepository,
            mapGptVoicePlayerOptions,
        )
    }

    private fun createCoreVoiceClient(): RemoteTTSApiClient {
        return CoreVoiceApiClient(
            coreHttpClient,
            languageRepository,
            mapGptVoicePlayerOptions,
        )
    }

    companion object {

        private const val TAG = "PlatformAudioPlayerFactory"

        /**
         * Create your own [SoundPlayer].
         */
        fun createSoundPlayer(
            context: Context,
            options: PlayerOptions = PlayerOptions.Builder().build(),
        ): SoundPlayer = MapGptSoundPlayer(
            context,
            VoiceInstructionsPlayerAttributesProvider.retrievePlayerAttributes(options),
        )
    }
}

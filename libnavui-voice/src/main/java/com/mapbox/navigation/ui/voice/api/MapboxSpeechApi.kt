package com.mapbox.navigation.ui.voice.api

import android.content.Context
import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.VoiceState
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Mapbox Speech Api that allows you to generate an announcement based on [VoiceInstructions]
 */
class MapboxSpeechApi internal constructor(
    private val context: Context,
    private val accessToken: String,
    private val language: String,
    private val options: MapboxSpeechApiOptions,
    internal val firstInstructionChecker: FirstVoiceInstructionsChecker
) {

    private val voiceInstructionsCache = VoiceInstructionsCache()
    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val predownloadJobController by lazy {
        InternalJobControlFactory.createDefaultScopeJobControl()
    }
    private val voiceAPI = VoiceApiProvider.retrieveMapboxVoiceApi(
        context,
        accessToken,
        language,
        options
    )
    private var firstInstructionDownloadJob: Job? = null

    /**
     * Creates Mapbox Speech Api instance.
     *
     * @param context Context
     * @param accessToken String
     * @param language [Locale] language (IETF BCP 47)
     * @param options [MapboxSpeechApiOptions] (optional)
     */
    @JvmOverloads constructor(
        context: Context,
        accessToken: String,
        language: String,
        options: MapboxSpeechApiOptions = MapboxSpeechApiOptions.Builder().build(),
    ) : this(context, accessToken, language, options, FirstVoiceInstructionsChecker())

    /**
     * Given [VoiceInstructions] the method will try to generate the
     * voice instruction [SpeechAnnouncement] including the synthesized speech mp3 file
     * from Mapbox's API Voice.
     * NOTE: this method will try downloading an mp3 file from server. If you use voice instructions
     * predownloading (see [VoiceInstructionsPrefetcher]), invoke [generatePredownloaded]
     * instead of this method in your [VoiceInstructionsObserver].
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param consumer is a [SpeechValue] including the announcement to be played when the
     * announcement is ready or a [SpeechError] including the error information and a fallback
     * with the raw announcement (without file) that can be played with a text-to-speech engine.
     * @see [cancel]
     */
    fun generate(
        voiceInstruction: VoiceInstructions,
        consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
    ) {
        mainJobController.scope.launch {
            consumer.accept(retrieveVoiceFile(voiceInstruction))
        }
    }

    /**
     * Given [VoiceInstructions] the method will try to generate the
     * voice instruction [SpeechAnnouncement] including the synthesized speech mp3 file
     * from Mapbox's API Voice.
     * NOTE: this method will NOT try downloading an mp3 file from server. It will either use
     * an already predownloaded file or an onboard speech synthesizer. Only invoke this method
     * if you use voice instructions predownloading (see [VoiceInstructionsPrefetcher]),
     * otherwise invoke [generatePredownloaded] in your [VoiceInstructionsObserver].
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param consumer is a [SpeechValue] including the announcement to be played when the
     * announcement is ready or a [SpeechError] including the error information and a fallback
     * with the raw announcement (without file) that can be played with a text-to-speech engine.
     * @see [cancel]
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun generatePredownloaded(
        voiceInstruction: VoiceInstructions,
        consumer: MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>
    ) {
        firstInstructionDownloadJob?.cancel()
        firstInstructionDownloadJob = null
        if (firstInstructionChecker.isFirstVoiceInstruction(voiceInstruction)) {
            firstInstructionDownloadJob = mainJobController.scope.launch {
                val cachedFile = try {
                    awaitCachedFile(voiceInstruction)
                } catch (ex: CancellationException) {
                    val fallback = getFallbackAnnouncement(voiceInstruction)
                    val error = SpeechError(
                        "Instruction download cancelled: a newer instruction has to be played",
                        ex,
                        fallback
                    )
                    consumer.accept(ExpectedFactory.createError(error))
                    throw ex
                }
                consumer.accept(cachedFile)
            }
        } else {
            mainJobController.scope.launch {
                val cachedValue = getFromCache(voiceInstruction)
                if (cachedValue != null) {
                    consumer.accept(cachedValue)
                } else {
                    val fallback = getFallbackAnnouncement(voiceInstruction)
                    val speechError = SpeechError(
                        "No predownloaded instruction for ${voiceInstruction.announcement()}",
                        null,
                        fallback
                    )
                    consumer.accept(ExpectedFactory.createError(speechError))
                }
            }
        }
    }

    /**
     * The method stops the process of retrieving the file voice instruction [SpeechAnnouncement]
     * and destroys any related callbacks.
     * @see [generate]
     */
    fun cancel() {
        mainJobController.job.children.forEach {
            it.cancel()
        }
        voiceAPI.cancel()
    }

    /**
     * Given the [SpeechAnnouncement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    fun clean(announcement: SpeechAnnouncement) {
        voiceAPI.clean(announcement)
        VoiceInstructionsParser.parse(announcement).onValue {
            val value = voiceInstructionsCache.get(it)
            // when we clear fallback announcement, there is a chance we will remove the key
            // from map and not remove the file itself
            // since for fallback SpeechAnnouncement file is null
            if (value?.value?.announcement == announcement) {
                voiceInstructionsCache.remove(it)
            }
        }
    }

    @UiThread
    internal fun predownload(instructions: List<VoiceInstructions>) {
        instructions.forEach { instruction ->
            val typeAndAnnouncement = VoiceInstructionsParser.parse(instruction).value
            if (
                typeAndAnnouncement != null &&
                voiceInstructionsCache.get(typeAndAnnouncement)?.isValue != true
            ) {
                predownloadJobController.scope.launch {
                    val voiceFile = retrieveVoiceFile(instruction)
                    mainJobController.scope.launch {
                        voiceInstructionsCache.put(typeAndAnnouncement, voiceFile)
                    }
                }
            }
        }
    }

    internal fun cancelPredownload() {
        predownloadJobController.job.children.forEach { it.cancel() }
        val announcements = voiceInstructionsCache.getEntries().mapNotNull {
            it.value.value?.announcement
        }
        announcements.forEach { clean(it) }
    }

    @Throws(IllegalStateException::class)
    private suspend fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
    ): Expected<SpeechError, SpeechValue> {
        when (val result = voiceAPI.retrieveVoiceFile(voiceInstruction)) {
            is VoiceState.VoiceFile -> {
                val announcement = voiceInstruction.announcement()
                val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                return ExpectedFactory.createValue(
                    SpeechValue(
                        // Can't be null as it's checked in retrieveVoiceFile
                        SpeechAnnouncement.Builder(announcement!!)
                            .ssmlAnnouncement(ssmlAnnouncement)
                            .file(result.instructionFile)
                            .build()
                    )
                )
            }
            is VoiceState.VoiceError -> {
                val fallback = getFallbackAnnouncement(voiceInstruction)
                val speechError = SpeechError(result.exception, null, fallback)
                return ExpectedFactory.createError(speechError)
            }
        }
    }

    @Throws(IllegalStateException::class)
    private fun getFallbackAnnouncement(voiceInstruction: VoiceInstructions): SpeechAnnouncement {
        VoiceInstructionsParser.parse(voiceInstruction).error?.also {
            throw IllegalStateException(it.message)
        }

        val announcement = voiceInstruction.announcement()
        val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
        // Can't be null as it's checked in VoiceInstructionsParser.parse
        return SpeechAnnouncement.Builder(announcement!!)
            .ssmlAnnouncement(ssmlAnnouncement)
            .build()
    }

    private fun getFromCache(
        voiceInstruction: VoiceInstructions
    ): Expected<SpeechError, SpeechValue>? {
        val key = VoiceInstructionsParser.parse(voiceInstruction).value
        return key?.let { voiceInstructionsCache.get(it) }
    }

    private suspend fun awaitCachedFile(voiceInstruction: VoiceInstructions) =
        suspendCancellableCoroutine<Expected<SpeechError, SpeechValue>> { cont ->
            val key = VoiceInstructionsParser.parse(voiceInstruction).value
            key?.let {
                val observer: (Expected<SpeechError, SpeechValue>) -> Unit = { speechValue ->
                    cont.resume(speechValue)
                }
                cont.invokeOnCancellation {
                    voiceInstructionsCache.unregisterOneShotObserver(key, observer)
                }
                voiceInstructionsCache.registerOneShotObserver(it, observer)
            }
        }
}

/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.ui.voice.api

import android.net.Uri
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.MalformedURLException
import java.net.URL
import kotlin.coroutines.resume

internal class MapboxSpeechLoader(
    private val accessToken: String,
    private val language: String,
    private val urlSkuTokenProvider: UrlSkuTokenProvider,
    private val options: MapboxSpeechApiOptions,
    private val resourceLoader: ResourceLoader,
) {

    private val downloadedInstructions = mutableSetOf<TypeAndAnnouncement>()
    private val downloadedInstructionsLock = Any()
    private val defaultScope = InternalJobControlFactory.createDefaultScopeJobControl().scope

    suspend fun load(
        voiceInstruction: VoiceInstructions,
        onlyCache: Boolean
    ): Expected<Throwable, ByteArray> {
        return runCatching {
            val typeAndAnnouncement = VoiceInstructionsParser.parse(voiceInstruction)
                .getValueOrElse { throw it }
            val request = createRequest(typeAndAnnouncement).apply {
                if (onlyCache) {
                    networkRestriction = NetworkRestriction.DISALLOW_ALL
                }
            }
            val response = resourceLoader.load(request)
            return processResponse(response)
        }.getOrElse {
            createError(it)
        }
    }

    fun triggerDownload(voiceInstructions: List<VoiceInstructions>) {
        voiceInstructions.forEach { voiceInstruction ->
            defaultScope.launch {
                val typeAndAnnouncement = VoiceInstructionsParser.parse(voiceInstruction).value
                if (typeAndAnnouncement != null && !hasTypeAndAnnouncement(typeAndAnnouncement)) {
                    predownload(typeAndAnnouncement)
                }
            }
        }
    }

    fun cancel() {
        defaultScope.cancel()
    }

    private fun hasTypeAndAnnouncement(typeAndAnnouncement: TypeAndAnnouncement): Boolean {
        synchronized(downloadedInstructionsLock) {
            return typeAndAnnouncement in downloadedInstructions
        }
    }

    private suspend fun predownload(typeAndAnnouncement: TypeAndAnnouncement) {
        try {
            suspendCancellableCoroutine { cont ->
                val request = createRequest(typeAndAnnouncement)
                val id = resourceLoader.load(request) { result ->
                    // tilestore thread
                    if (result.isValue) {
                        synchronized(downloadedInstructionsLock) {
                            downloadedInstructions.add(typeAndAnnouncement)
                        }
                    }
                    cont.resume(Unit)
                }
                cont.invokeOnCancellation { resourceLoader.cancel(id) }
            }
        } catch (ex: Throwable) {
            logE("Failed to download instruction '$typeAndAnnouncement': ${ex.localizedMessage}")
        }
    }

    private fun createRequest(typeAndAnnouncement: TypeAndAnnouncement): ResourceLoadRequest {
        val url = instructionUrl(typeAndAnnouncement.announcement, typeAndAnnouncement.type)
        return ResourceLoadRequest(url).apply {
            flags = ResourceLoadFlags.ACCEPT_EXPIRED
        }
    }

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>
    ): Expected<Throwable, ByteArray> =
        response.fold({
            createError("${it.type}: ${it.message}")
        }, {
            when (it.status) {
                ResourceLoadStatus.AVAILABLE -> {
                    val blob: ByteArray = it.data?.data ?: byteArrayOf()
                    if (blob.isNotEmpty()) {
                        createValue(blob)
                    } else {
                        createError("No data available.")
                    }
                }
                ResourceLoadStatus.UNAUTHORIZED ->
                    createError("Your token cannot access this resource.")
                ResourceLoadStatus.NOT_FOUND ->
                    createError("Resource is missing.")
                else ->
                    createError("Unknown error (status: ${it.status}).")
            }
        })

    @Throws(MalformedURLException::class)
    private fun instructionUrl(instruction: String, textType: String): String {
        val authority = options.baseUri.split("//")
            .lastOrNull() ?: throw MalformedURLException("Invalid base url")

        val encodedInstruction = UrlUtils.encodePathSegment(instruction)
        val uri = Uri.Builder()
            .scheme("https")
            .authority(authority)
            .appendEncodedPath("$VOICE_REQUEST_PATH/$encodedInstruction")
            .appendQueryParameter("textType", textType)
            .appendQueryParameter("language", language)
            .appendQueryParameter("access_token", accessToken)
        // omitting "outputFormat" -> will default to MP3

        val resourceUrl = URL(uri.build().toString()) // throws MalformedURLException
        return urlSkuTokenProvider.obtainUrlWithSkuToken(resourceUrl).toString()
    }

    private fun createError(message: String): Expected<Throwable, ByteArray> {
        return createError(Error(message))
    }

    private companion object {
        private const val VOICE_REQUEST_PATH = "voice/v1/speak"
    }
}

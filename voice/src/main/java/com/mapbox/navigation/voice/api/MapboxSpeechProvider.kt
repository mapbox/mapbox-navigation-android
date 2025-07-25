/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.voice.api

import android.net.Uri
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.MapboxOptions
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.ui.base.util.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.utils.internal.ByteBufferBackedInputStream
import com.mapbox.navigation.utils.internal.UrlUtils
import com.mapbox.navigation.voice.model.TypeAndAnnouncement
import com.mapbox.navigation.voice.options.MapboxSpeechApiOptions
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL

internal class MapboxSpeechProvider(
    private val language: String,
    private val urlSkuTokenProvider: UrlSkuTokenProvider,
    private val options: MapboxSpeechApiOptions,
    private val resourceLoader: ResourceLoader,
) {

    suspend fun load(voiceInstruction: VoiceInstructions): Expected<Throwable, InputStream> {
        return runCatching {
            val typeAndAnnouncement = VoiceInstructionsParser.parse(voiceInstruction)
                .getValueOrElse { throw it }
            val request = createRequest(typeAndAnnouncement)
            val response = resourceLoader.load(request)
            return processResponse(response)
        }.getOrElse {
            createError(it)
        }
    }

    private fun createRequest(typeAndAnnouncement: TypeAndAnnouncement): ResourceLoadRequest {
        val url = instructionUrl(typeAndAnnouncement.announcement, typeAndAnnouncement.type)
        return ResourceLoadRequest(url).apply {
            flags = ResourceLoadFlags.ACCEPT_EXPIRED
        }
    }

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>,
    ): Expected<Throwable, InputStream> =
        response.fold({
            createError("${it.type}: ${it.message}")
        }, {
            when (it.status) {
                ResourceLoadStatus.AVAILABLE -> {
                    val resourceDataBuffer = it.data?.data?.buffer
                    if (resourceDataBuffer != null) {
                        createValue(ByteBufferBackedInputStream(resourceDataBuffer))
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
        },)

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
            .appendQueryParameter("access_token", MapboxOptions.accessToken)
            .apply {
                options.gender?.let {
                    appendQueryParameter("gender", it)
                }
            }
        // omitting "outputFormat" -> will default to MP3

        val resourceUrl = URL(uri.build().toString()) // throws MalformedURLException
        return urlSkuTokenProvider.obtainUrlWithSkuToken(resourceUrl).toString()
    }

    private fun createError(message: String): Expected<Throwable, InputStream> {
        return createError(Error(message))
    }

    private companion object {
        private const val VOICE_REQUEST_PATH = "voice/v1/speak"
    }
}

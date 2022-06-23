/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.ui.voice.api

import android.net.Uri
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement
import com.mapbox.navigation.ui.voice.options.MapboxSpeechApiOptions
import java.net.MalformedURLException
import java.net.URL

internal class MapboxSpeechProvider(
    private val accessToken: String,
    private val language: String,
    private val urlSkuTokenProvider: UrlSkuTokenProvider,
    private val options: MapboxSpeechApiOptions,
    private val resourceLoader: ResourceLoader
) {

    suspend fun load(typeAndAnnouncement: TypeAndAnnouncement): Expected<Throwable, ByteArray> {
        return runCatching {
            val url = instructionUrl(typeAndAnnouncement.announcement, typeAndAnnouncement.type)
            val response = resourceLoader.load(url)
            return processResponse(response)
        }.getOrElse {
            createError(it)
        }
    }

    private suspend fun ResourceLoader.load(url: String) = load(ResourceLoadRequest(url))

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>
    ): Expected<Throwable, ByteArray> =
        response.value?.let { responseData ->
            when (responseData.status) {
                ResourceLoadStatus.AVAILABLE -> {
                    val blob: ByteArray = responseData.data?.data ?: byteArrayOf()
                    if (blob.isNotEmpty()) createValue(blob)
                    else createError("No data available.")
                }
                ResourceLoadStatus.UNAUTHORIZED ->
                    createError("Your token cannot access this resource.")
                ResourceLoadStatus.NOT_FOUND ->
                    createError("Resource is missing.")
                else ->
                    createError("Unknown error (status: ${responseData.status}).")
            }
        } ?: createError(response.error?.message ?: "No data available.")

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

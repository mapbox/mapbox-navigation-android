package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterface
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.HttpException
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.navigator.offline.TilesetVersionsApi.RouteTileVersionsResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.io.IOException

internal typealias ExpectedRouteTileVersionsCallback =
    Expected<Throwable, RouteTileVersionsResponse>

@OptIn(ExperimentalSerializationApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
internal class TilesetVersionsApi(
    private val httpService: HttpServiceInterface = HttpServiceFactory.getInstance(),
) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun getRouteTileVersions(
        baseUri: String,
        dataset: String,
        profile: String,
        callback: (ExpectedRouteTileVersionsCallback) -> Unit,
    ): Cancelable {
        val url = buildVersionsApiUrl(
            baseUri = baseUri,
            dataset = dataset,
            profile = profile,
        )
        val request = buildGetRequest(url)
        return executeCall(request, RouteTileVersionsResponse.serializer(), callback)
    }

    private fun <T : Any> executeCall(
        request: HttpRequest,
        serializer: KSerializer<T>,
        callback: (Expected<Throwable, T>) -> Unit,
    ): Cancelable {
        val requestId = httpService.request(
            request,
        ) { response ->
            response.result.fold(
                { error ->
                    callback(ExpectedFactory.createError(IOException(error.toString())))
                },
                { value ->
                    try {
                        val byteStream = ByteArrayInputStream(value.data)
                        if (value.code in 200..299) {
                            val result = json.decodeFromStream(serializer, byteStream)
                            callback(ExpectedFactory.createValue(result))
                        } else {
                            callback(
                                ExpectedFactory.createError(
                                    HttpException(
                                        httpCode = value.code,
                                        message = String(value.data),
                                    ),
                                ),
                            )
                        }
                    } catch (exception: SerializationException) {
                        callback(
                            ExpectedFactory.createError(
                                RuntimeException(
                                    "Cannot parse route tile versions data",
                                    exception,
                                ),
                            ),
                        )
                    } catch (exception: Exception) {
                        callback(ExpectedFactory.createError(exception))
                    }
                },
            )
        }

        return Cancelable {
            httpService.cancelRequest(requestId) {
                // do nothing
            }
        }
    }

    @Serializable
    data class RouteTileVersionsResponse(
        val availableVersions: List<String> = emptyList(),
        val blockedVersions: Set<String> = emptySet(),
    )

    companion object {

        private fun buildVersionsApiUrl(
            baseUri: String,
            dataset: String,
            profile: String,
        ): String {
            return "$baseUri/route-tiles/v2/$dataset/$profile/" +
                "versions?access_token=${MapboxOptions.accessToken}"
        }

        private fun buildGetRequest(url: String) = HttpRequest.Builder()
            .headers(hashMapOf())
            .url(url)
            .sdkInformation(SdkInfoProvider.sdkInformation())
            .method(HttpMethod.GET)
            .build()
    }
}

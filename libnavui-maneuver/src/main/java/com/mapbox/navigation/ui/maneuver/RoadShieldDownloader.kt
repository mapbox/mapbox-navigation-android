package com.mapbox.navigation.ui.maneuver

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.UAComponents
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal object RoadShieldDownloader {

    private const val SVG_EXTENSION = ".svg"
    private const val USER_AGENT_KEY = "User-Agent"
    private const val USER_AGENT_VALUE = "MapboxJava/"
    private const val SDK_IDENTIFIER = "mapbox-navigation-ui-android"

    private const val CODE_200 = 200L
    private const val CODE_401 = 401L
    private const val CODE_404 = 404L

    suspend fun downloadImage(imageUrl: String): Expected<String, ByteArray> =
        suspendCancellableCoroutine { continuation ->
            val id = CommonSingletonModuleProvider.httpServiceInstance.request(
                getHttpRequest(imageUrl)
            ) { response ->
                when {
                    response.result.isValue -> {
                        response.result.value?.let { responseData ->
                            when (responseData.code) {
                                CODE_200 -> {
                                    continuation.resume(
                                        ExpectedFactory.createValue(
                                            responseData.data
                                        )
                                    )
                                }
                                CODE_401 -> {
                                    continuation.resume(
                                        ExpectedFactory.createError(
                                            "Your token cannot access this resource."
                                        )
                                    )
                                }
                                CODE_404 -> {
                                    continuation.resume(
                                        ExpectedFactory.createError(
                                            "Resource is missing."
                                        )
                                    )
                                }
                                else -> {
                                    continuation.resume(
                                        ExpectedFactory.createError(
                                            "Unknown error (code: ${responseData.code})."
                                        )
                                    )
                                }
                            }
                        } ?: continuation.resume(
                            ExpectedFactory.createError(
                                "No data available."
                            )
                        )
                    }
                    response.result.isError -> {
                        continuation.resume(
                            ExpectedFactory.createError(response.result.error?.message ?: "")
                        )
                    }
                }
            }
            continuation.invokeOnCancellation {
                CommonSingletonModuleProvider.httpServiceInstance.cancelRequest(id) {}
            }
        }

    private fun getHttpRequest(imageUrl: String): HttpRequest {
        return HttpRequest.Builder()
            .url(imageUrl.plus(SVG_EXTENSION))
            .body(byteArrayOf())
            .headers(
                hashMapOf(
                    Pair(
                        USER_AGENT_KEY,
                        USER_AGENT_VALUE
                    )
                )
            )
            .method(HttpMethod.GET)
            .uaComponents(
                UAComponents.Builder()
                    .sdkIdentifierComponent(SDK_IDENTIFIER)
                    .build()
            )
            .build()
    }
}

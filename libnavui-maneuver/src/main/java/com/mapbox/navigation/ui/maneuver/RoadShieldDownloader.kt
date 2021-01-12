package com.mapbox.navigation.ui.maneuver

import com.mapbox.common.HttpRequest
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.ui.maneuver.model.OnRoadShieldDownload
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object RoadShieldDownloader {

    private const val CODE_200 = 200L
    private const val CODE_401 = 401L
    private const val CODE_404 = 404L

    suspend fun downloadImage(request: HttpRequest): OnRoadShieldDownload =
        suspendCoroutine { continuation ->
            CommonSingletonModuleProvider.httpServiceInstance.request(request) { response ->
                when {
                    response.result.isValue -> {
                        response.result.value?.let { responseData ->
                            when (responseData.code) {
                                CODE_200 -> {
                                    continuation.resume(OnRoadShieldDownload(responseData.data))
                                }
                                CODE_401 -> {
                                    continuation.resume(
                                        OnRoadShieldDownload(
                                            null,
                                            "Your token cannot access this resource."
                                        )
                                    )
                                }
                                CODE_404 -> {
                                    continuation.resume(
                                        OnRoadShieldDownload(
                                            null,
                                            "Resource is missing."
                                        )
                                    )
                                }
                                else -> {
                                    continuation.resume(
                                        OnRoadShieldDownload(
                                            null,
                                            "Unknown error."
                                        )
                                    )
                                }
                            }
                        } ?: continuation.resume(
                            OnRoadShieldDownload(
                                null,
                                "No data available."
                            )
                        )
                    }
                    response.result.isError -> {
                        continuation.resume(
                            OnRoadShieldDownload(null, response.result.error?.message)
                        )
                    }
                }
            }
        }
}

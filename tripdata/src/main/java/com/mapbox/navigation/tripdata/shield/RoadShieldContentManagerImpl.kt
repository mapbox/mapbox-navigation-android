package com.mapbox.navigation.tripdata.shield

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldOrigin
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 *
 * The object makes use of the data from [BannerComponents.imageBaseUrl] and
 * [BannerComponents.mapboxShield] to download legacy road shields or mapbox designed road shields
 * if the values are available. The logic on how the utility decided which one to use is described
 * below:
 *
 * For a given sample instruction
 *
 * bannerInstructions: {
 *    primary: {
 *      components: [
 *          {
 *              type: text
 *              text: Fremont Street
 *          },
 *          {
 *              imageBaseUrl: https://mapbox.navigation.shields.s3.....
 *              mapbox_shield: {
 *                  base_url: https://api.mapbox.com/styles/v1
 *                  text_color: black
 *                  display_ref: 880
 *                  shieldName: us-interstate
 *              }
 *              type: icon
 *              text: I880
 *          }
 *      ]
 *    }
 * }
 *
 * 1. If imageBaseUrl is non null, mapbox shield is null:
 *     - Request shield using imageBaseUrl:
 *       - If request success: return legacy shield
 *       - If request fails: return error
 * 2. If mapbox shield is non null, imageBaseUrl is null
 *     - If sprite does not exist for shield url: return error
 *       - If sprite exists:
 *         - If placeholder does not exist: return error
 *         - If placeholder exists:
 *           - Request shield using mapbox shield:
 *             - If request success: return mapbox shield
 *             - If request fails: return error
 * 3. If mapbox shield & imageBaseUrl is non null
 *     - If sprite does not exist for shield url: return error
 *       - If sprite exists:
 *         - If placeholder does not exist: return error
 *         - If placeholder exists:
 *           - Request shield using mapbox shield:
 *             - If request success: return mapbox shield
 *             - If request fails: repeat step 1.
 */
internal class RoadShieldContentManagerImpl(
    private val shieldResultCache: ShieldResultCache = ShieldResultCache(),
) : RoadShieldContentManager {
    internal companion object {
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val resultMap =
        hashMapOf<ShieldRequest, Expected<RouteShieldError, RouteShieldResult>>()

    private val mainJob = InternalJobControlFactory.createMainScopeJobControl()
    private val awaitingCallbacks = mutableListOf<() -> Boolean>()

    override suspend fun getShields(
        shieldsToDownload: List<RouteShieldToDownload>,
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        val requests = prepareShields(shieldsToDownload)
        return try {
            waitForShields(requests)
        } catch (ex: CancellationException) {
            val availableResults = requests.filter { resultMap.containsKey(it) }
            val missingResults = requests.filterNot { availableResults.contains(it) }

            val returnList = mutableListOf<Expected<RouteShieldError, RouteShieldResult>>()
            availableResults.forEach {
                returnList.add(resultMap.remove(it)!!)
            }
            returnList.addAll(
                missingResults.map {
                    ExpectedFactory.createError(
                        RouteShieldError(null, CANCELED_MESSAGE),
                    )
                },
            )
            return returnList
        }
    }

    override fun cancelAll() {
        mainJob.job.children.forEach { it.cancel() }
    }

    private fun prepareShields(
        shieldsToDownload: List<RouteShieldToDownload>,
    ): Set<ShieldRequest> {
        return shieldsToDownload.map { toDownload ->
            val request = ShieldRequest(toDownload)
            mainJob.scope.launch {
                when (toDownload) {
                    is RouteShieldToDownload.MapboxDesign -> {
                        val mapboxDesignShieldResult = shieldResultCache.getOrRequest(toDownload)
                        resultMap[request] = if (mapboxDesignShieldResult.isError) {
                            val designError = mapboxDesignShieldResult.error!!.error
                            val designUrl = mapboxDesignShieldResult.error!!.requestUrl
                            val legacyFallback = toDownload.legacyFallback
                            if (legacyFallback != null) {
                                shieldResultCache.getOrRequest(legacyFallback).fold(
                                    { error ->
                                        ExpectedFactory.createError(
                                            RouteShieldError(
                                                url = designUrl,
                                                errorMessage = """
                                                    |original request failed with:
                                                    |url: $designUrl
                                                    |error: $designError
                                                    |
                                                    |fallback request failed with:
                                                    |url: ${error.requestUrl}
                                                    |error: ${error.error}
                                                """.trimMargin(),
                                            ),
                                        )
                                    },
                                    { legacyShield ->
                                        ExpectedFactory.createValue(
                                            RouteShieldResult(
                                                legacyShield.response,
                                                RouteShieldOrigin(
                                                    isFallback = true,
                                                    originalUrl = designUrl,
                                                    designError,
                                                ),
                                            ),
                                        )
                                    },
                                )
                            } else {
                                ExpectedFactory.createError(
                                    RouteShieldError(
                                        url = designUrl,
                                        errorMessage = designError,
                                    ),
                                )
                            }
                        } else {
                            ExpectedFactory.createValue(
                                RouteShieldResult(
                                    mapboxDesignShieldResult.value!!.response,
                                    RouteShieldOrigin(
                                        isFallback = false,
                                        mapboxDesignShieldResult.value!!.requestUrl,
                                        "",
                                    ),
                                ),
                            )
                        }
                    }
                    is RouteShieldToDownload.MapboxLegacy -> {
                        resultMap[request] = shieldResultCache.getOrRequest(toDownload).fold(
                            { error ->
                                ExpectedFactory.createError(
                                    RouteShieldError(
                                        url = error.requestUrl,
                                        errorMessage = error.error,
                                    ),
                                )
                            },
                            { legacyShield ->
                                ExpectedFactory.createValue(
                                    RouteShieldResult(
                                        legacyShield.response,
                                        RouteShieldOrigin(
                                            isFallback = false,
                                            originalUrl = legacyShield.requestUrl,
                                            originalErrorMessage = "",
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                }
                invalidate()
            }
            request
        }.toSet()
    }

    private fun invalidate() {
        val iterator = awaitingCallbacks.iterator()
        while (iterator.hasNext()) {
            val remove = iterator.next().invoke()
            if (remove) {
                iterator.remove()
            }
        }
    }

    private suspend fun waitForShields(
        requests: Set<ShieldRequest>,
    ): List<Expected<RouteShieldError, RouteShieldResult>> {
        return suspendCancellableCoroutine { continuation ->
            val callback = {
                check(!continuation.isCancelled)
                if (requests.all { request -> resultMap.containsKey(request) }) {
                    val returnList = mutableListOf<Expected<RouteShieldError, RouteShieldResult>>()
                    requests.forEach {
                        returnList.add(resultMap.remove(it)!!)
                    }
                    continuation.resume(returnList)
                    true
                } else {
                    false
                }
            }
            if (callback()) {
                return@suspendCancellableCoroutine
            }
            awaitingCallbacks.add(callback)
            continuation.invokeOnCancellation {
                awaitingCallbacks.remove(callback)
            }
        }
    }
}

private class ShieldRequest(
    val toDownload: RouteShieldToDownload,
) {
    val id: UUID = UUID.randomUUID()

    /**
     * Intentionally only [id] is used for hash so that it's the only element
     * in a key for maps.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }

    /**
     * Intentionally only [id] is used for equality check so that it's the only element
     * in a key for maps.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShieldRequest

        if (id != other.id) return false

        return true
    }

    override fun toString(): String {
        return "ShieldRequest(toDownload=$toDownload, id=$id)"
    }
}

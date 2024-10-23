@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.toDirectionsResponse
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigator.RouteInterface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer

private const val LOG_TAG = "RouteParsingManager"
private const val RESPONSE_SIZE_TO_OPTIMIZE_BYTES = 20 * 1024 * 1024 // 20 MB

sealed class AlternativesParsingResult<out T> {
    object NotActual : AlternativesParsingResult<Nothing>()
    data class Parsed<T>(val value: T) : AlternativesParsingResult<T>()
}

typealias PrepareForParsingAction = suspend () -> Unit

interface RouteParsingManager {
    fun setPrepareForParsingAction(action: PrepareForParsingAction)

    suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend () -> T,
    ): T

    suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend () -> T,
    ): AlternativesParsingResult<T>

    fun parseRouteToDirections(route: RouteInterface): DirectionsResponse {
        logD(LOG_TAG) { "Parsing directions response for routeId = ${route.routeId}" }
        return route.responseJsonRef.toDirectionsResponse()
    }
}

fun createRouteParsingManager(): RouteParsingManager {
    return OptimisedRoutesParsingManager()
}

private class OptimisedRoutesParsingManager() : RouteParsingManager {

    private val mutex = Mutex()

    private var prepareForParsingAction: PrepareForParsingAction = { }

    override fun setPrepareForParsingAction(action: PrepareForParsingAction) {
        prepareForParsingAction = action
    }

    override suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend () -> T,
    ): T {
        return if (routeResponseInfo.sizeBytes < RESPONSE_SIZE_TO_OPTIMIZE_BYTES) {
            parsing()
        } else {
            logD(LOG_TAG) { "Enqueuing routes parsing" }
            mutex.withLock {
                prepareForParsing()
                parsing()
            }
        }
    }

    override suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend () -> T,
    ): AlternativesParsingResult<T> {
        return if (mutex.isLocked) {
            logD(LOG_TAG) {
                "skipping parsing of routes alternatives" +
                    " as a different route is being parsed already"
            }
            AlternativesParsingResult.NotActual
        } else {
            AlternativesParsingResult.Parsed(
                parseRouteResponse(
                    arguments.routeResponseInfo,
                    parsing,
                ),
            )
        }
    }

    private suspend fun prepareForParsing() {
        logD(LOG_TAG) {
            "Preparing for routes response parsing"
        }
        prepareForParsingAction()
        logD(LOG_TAG) {
            "Preparation for routes parsing completed"
        }
    }
}

data class AlternativesInfo(
    val routeResponseInfo: RouteResponseInfo,
)

data class RouteResponseInfo(
    val sizeBytes: Int,
) {
    companion object {
        fun fromResponse(response: ByteBuffer) =
            RouteResponseInfo(sizeBytes = response.capacity())

        fun fromResponses(responses: List<ByteBuffer>) =
            RouteResponseInfo(sizeBytes = responses.maxOf { it.capacity() })
    }
}

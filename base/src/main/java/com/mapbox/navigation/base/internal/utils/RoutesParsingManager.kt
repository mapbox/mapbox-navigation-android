@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.utils.internal.logD
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface RouteParsingQueue {
    suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend () -> T,
    ): T

    suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend () -> T,
    ): AlternativesParsingResult<T>
}

fun createOptimizedRoutesParsingQueue(
    prepareForParsingAction: PrepareForParsingAction,
): RouteParsingQueue {
    return OptimisedForJavaMemoryRoutesParsingQueue(prepareForParsingAction)
}

fun createImmediateNoOptimizationsParsingQueue(): RouteParsingQueue =
    ImmediateNoOptimizationsParsingQueue()

private class ImmediateNoOptimizationsParsingQueue() : RouteParsingQueue {
    override suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend () -> T,
    ): T = parsing.invoke()

    override suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend () -> T,
    ): AlternativesParsingResult<T> {
        return AlternativesParsingResult.Parsed(parsing.invoke())
    }
}

private class OptimisedForJavaMemoryRoutesParsingQueue(
    private val prepareForParsingAction: PrepareForParsingAction,
) : RouteParsingQueue {

    private val mutex = Mutex()

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
            RouteResponseInfo(sizeBytes = responses.maxOfOrNull { it.capacity() } ?: 0)
    }
}

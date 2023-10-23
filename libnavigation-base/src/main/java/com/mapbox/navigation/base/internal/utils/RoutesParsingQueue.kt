package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.LongRoutesOptimisationOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer

sealed class AlternativesParsingResult<out T> {
    object NotActual : AlternativesParsingResult<Nothing>()
    data class Parsed<T>(val value: T) : AlternativesParsingResult<T>()
}

typealias PrepareForParsingAction = suspend () -> Unit

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesParsingQueue(
    private val longRoutesOptimisationOptions: LongRoutesOptimisationOptions
) {

    private val mutex = Mutex()

    private var prepareForParsingAction: PrepareForParsingAction = { }

    fun setPrepareForParsingAction(action: PrepareForParsingAction) {
        prepareForParsingAction = action
    }

    // TODO: add arguments and prepare for parsing only if that makes sense
    suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend (ParseArguments) -> T
    ): T {
        return if (longRoutesOptimisationOptions is LongRoutesOptimisationOptions.NoOptimisations) {
            parsing(ParseArguments(optimiseDirectionsResponseStructure = false))
        } else {
            mutex.withLock {
                prepareForParsingAction()
                parsing(ParseArguments(optimiseDirectionsResponseStructure = true))
            }
        }
    }

    suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend (ParseArguments) -> T
    ): AlternativesParsingResult<T> {
        if (arguments.userTriggeredAlternativesRefresh && longRoutesOptimisationOptions !is LongRoutesOptimisationOptions.NoOptimisations) {
            return AlternativesParsingResult.NotActual
        }
        return if (mutex.isLocked) {
            AlternativesParsingResult.NotActual
        } else {
            AlternativesParsingResult.Parsed(
                parseRouteResponse(
                    arguments.routeResponseInfo,
                    parsing
                )
            )
        }
    }
}

data class AlternativesInfo(
    val routeResponseInfo: RouteResponseInfo,
    val userTriggeredAlternativesRefresh: Boolean
)

data class RouteResponseInfo(
    val sizeBytes: Int
) {
    companion object {
        fun fromResponse(response: ByteBuffer) =
            RouteResponseInfo(sizeBytes = response.capacity())
    }
}

data class ParseArguments(
    val optimiseDirectionsResponseStructure: Boolean
)
@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

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

interface RouteParsingManager {
    fun setPrepareForParsingAction(action: PrepareForParsingAction)

    suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend (ParseArguments) -> T
    ): T

    suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend (ParseArguments) -> T
    ): AlternativesParsingResult<T>
}

fun createRouteParsingManager(longRoutesOptimisationOptions: LongRoutesOptimisationOptions): RouteParsingManager {
    return when (longRoutesOptimisationOptions) {
        LongRoutesOptimisationOptions.NoOptimisations -> NotOptimisedRoutesParsingManager()
        is LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes -> OptimisedRoutesParsingManager(longRoutesOptimisationOptions)
    }
}

private class OptimisedRoutesParsingManager(
    private val options: LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes
) : RouteParsingManager {

    private val mutex = Mutex()

    private var prepareForParsingAction: PrepareForParsingAction = { }

    override fun setPrepareForParsingAction(action: PrepareForParsingAction) {
        prepareForParsingAction = action
    }

    override suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend (ParseArguments) -> T
    ): T {
        return if (routeResponseInfo.sizeBytes < options.responseToParseSizeBytes) {
            parsing(ParseArguments(optimiseDirectionsResponseStructure = true))
        } else {
            mutex.withLock {
                prepareForParsingAction()
                parsing(ParseArguments(optimiseDirectionsResponseStructure = true))
            }
        }
    }

    override suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend (ParseArguments) -> T
    ): AlternativesParsingResult<T> {
        return if (arguments.userTriggeredAlternativesRefresh) {
            AlternativesParsingResult.NotActual
        } else if (mutex.isLocked) {
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

private class NotOptimisedRoutesParsingManager : RouteParsingManager {
    override fun setPrepareForParsingAction(action: PrepareForParsingAction) {
        // this implementation never triggers preparation
    }

    override suspend fun <T> parseRouteResponse(
        routeResponseInfo: RouteResponseInfo,
        parsing: suspend (ParseArguments) -> T
    ): T {
        return parsing(getParsingArgs())
    }

    override suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend (ParseArguments) -> T
    ): AlternativesParsingResult<T> {
        return AlternativesParsingResult.Parsed(
            parseRouteResponse(arguments.routeResponseInfo, parsing)
        )
    }

    private fun getParsingArgs() = ParseArguments(
        optimiseDirectionsResponseStructure = false
    )
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

@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.LongRoutesOptimisationOptions
import com.mapbox.navigation.utils.internal.logI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer

private const val LOG_TAG = "RouteParsingManager"

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

fun createRouteParsingManager(
    longRoutesOptimisationOptions: LongRoutesOptimisationOptions
): RouteParsingManager {
    return when (longRoutesOptimisationOptions) {
        LongRoutesOptimisationOptions.NoOptimisations -> NotOptimisedRoutesParsingManager()
        is LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes ->
            OptimisedRoutesParsingManager(longRoutesOptimisationOptions)
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
            logI(LOG_TAG) { "Starting parsing" }
            parsing(ParseArguments(optimiseDirectionsResponseStructure = true)).also {
                logI(LOG_TAG) { "Finished parsing" }
            }
        } else {
            logI(LOG_TAG) { "Enqueuing routes parsing" }
            mutex.withLock {
                prepareForParsing()
                logI(LOG_TAG) { "Starting parsing" }
                parsing(ParseArguments(optimiseDirectionsResponseStructure = true)).also {
                    logI(LOG_TAG) { "Finished parsing" }
                }
            }
        }
    }

    override suspend fun <T> parseAlternatives(
        arguments: AlternativesInfo,
        parsing: suspend (ParseArguments) -> T
    ): AlternativesParsingResult<T> {
        return if (arguments.userTriggeredAlternativesRefresh) {
            logI(LOG_TAG) { "skipping parsing of immediate route alternatives response" }
            AlternativesParsingResult.NotActual
        } else if (mutex.isLocked) {
            logI(LOG_TAG) {
                "skipping parsing of routes alternatives" +
                    " as a different route is being parsed already"
            }
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

    private suspend fun prepareForParsing() {
        logI(LOG_TAG) {
            "Preparing for routes response parsing"
        }
        prepareForParsingAction()
        logI(LOG_TAG) {
            "Preparation for routes parsing completed"
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

        fun fromResponses(responses: List<ByteBuffer>) =
            RouteResponseInfo(sizeBytes = responses.maxOf { it.capacity() })
    }
}

data class ParseArguments(
    val optimiseDirectionsResponseStructure: Boolean
)

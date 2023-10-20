package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.LongRoutesOptimisationOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    // TODO: add arguments and prepare for parsing only if that makes sence
    suspend fun <T> parseRouteResponse(parsing: suspend () -> T): T {
        return if (longRoutesOptimisationOptions is LongRoutesOptimisationOptions.NoOptimisations) {
            parsing()
        } else {
            mutex.withLock {
                prepareForParsingAction()
                parsing()
            }
        }
    }

    suspend fun <T> parseAlternatives(
        arguments: ParseAlternativesArguments,
        parsing: suspend () -> T
    ): AlternativesParsingResult<T> {
        if (arguments.userTriggeredAlternativesRefresh && longRoutesOptimisationOptions !is LongRoutesOptimisationOptions.NoOptimisations) {
            return AlternativesParsingResult.NotActual
        }
        return if (mutex.isLocked) {
            AlternativesParsingResult.NotActual
        } else {
            AlternativesParsingResult.Parsed(parseRouteResponse(parsing))
        }
    }
}

data class ParseAlternativesArguments(
    val newResponseSizeBytes: Int,
    val currentRouteLength: Double,
    val userTriggeredAlternativesRefresh: Boolean
)
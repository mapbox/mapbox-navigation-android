package com.mapbox.navigation.base.internal.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class AlternativesParsingResult<out T> {
    object NotActual : AlternativesParsingResult<Nothing>()
    data class Parsed<T>(val value: T) : AlternativesParsingResult<T>()
}

typealias PrepareForParsingAction = suspend () -> Unit

class RoutesParsingQueue {

    private val mutex = Mutex()

    private var prepareForParsingAction: PrepareForParsingAction = { }

    fun setPrepareForParsingAction(action: PrepareForParsingAction) {
        prepareForParsingAction = action
    }

    suspend fun <T> parseRouteResponse(parsing: suspend () -> T): T {
        return mutex.withLock {
            prepareForParsingAction()
            parsing()
        }
    }

    suspend fun <T> parseAlternatives(parsing: suspend () -> T): AlternativesParsingResult<T> {
        return if (mutex.isLocked) {
            AlternativesParsingResult.NotActual
        } else {
            AlternativesParsingResult.Parsed(parseRouteResponse(parsing))
        }
    }
}

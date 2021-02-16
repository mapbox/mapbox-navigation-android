package com.mapbox.navigation.ui.base.example

sealed class Expected<out V, out E>
data class Success<out V>(val value: V) : Expected<V, Nothing>()
data class Failure<out E>(val error: E) : Expected<Nothing, E>()

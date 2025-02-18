package com.mapbox.navigation.mapgpt.core.api.native

class NativeSessionConnectionException(
    val error: NativeSessionConnectionError,
) : RuntimeException("Native session connection exception: $error")

internal fun NativeSessionConnectionError.toException(): Throwable =
    NativeSessionConnectionException(this)

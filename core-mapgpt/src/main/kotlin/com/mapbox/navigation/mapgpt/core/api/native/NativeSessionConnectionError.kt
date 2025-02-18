package com.mapbox.navigation.mapgpt.core.api.native

sealed class NativeSessionConnectionError(private val name: String) {
    object NotConnectedError : NativeSessionConnectionError("NotConnectedError")
    object AlreadyConnectedError : NativeSessionConnectionError("AlreadyConnectedError")
    object HttpError : NativeSessionConnectionError("HttpError")
    object WssError : NativeSessionConnectionError("WssError")
    object InvalidResponseError : NativeSessionConnectionError("InvalidResponseError")
    object OtherError : NativeSessionConnectionError("OtherError")
    object WrongSessionTypeError : NativeSessionConnectionError("WrongSessionTypeError")

    override fun toString(): String = name
}

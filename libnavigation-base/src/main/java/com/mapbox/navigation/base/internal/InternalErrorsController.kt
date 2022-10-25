package com.mapbox.navigation.base.internal

object InternalErrorsController {

    private var listener: InternalErrorListener = InternalErrorListener { }

    fun setInternalErrorListener(listener: InternalErrorListener) {
        InternalErrorsController.listener = listener
    }

    fun onInternalError(error: Throwable) {
        listener.onInternalSDKError(error)
    }

    fun onInternalError(error: String) {
        listener.onInternalSDKError(Throwable(error))
    }
}

fun interface InternalErrorListener {
    fun onInternalSDKError(error: Throwable)
}
package com.mapbox.navigation.android

import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class MapboxLogger {
  interface Delegate {
    fun log(tag: String, message: String, cause: Throwable? = null)
  }

  companion object {
    private val DELEGATE: AtomicReference<Delegate> = AtomicReference()

    fun setDelegate(delegate: Delegate) {
      DELEGATE.set(delegate)
    }

    fun log(tag: String, message: String) {
      log(tag, message)
    }

    fun log(tag: String, message: String, cause: Throwable? = null) {
      Timber.e(cause, tag, message)
      DELEGATE.get()?.log(tag, message, cause)
    }
  }
}
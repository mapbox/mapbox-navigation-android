package com.mapbox.navigation.core.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocales
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

internal class SystemLocaleWatcher private constructor(
    private val context: Context,
    private val navigator: MapboxNativeNavigator,
    private val handler: Handler,
) {

    private var overrideLanguages: List<String>? = null
    private var isLocaleChangeReceiverRegistered = false
    private var isDestroyed = false

    private val localeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // When we receive this event, the system's configuration update might not have been
            // fully propagated, so we introduce a small delay before we update language
            handler.postDelayed(::updateUserLanguages, LOCALE_UPDATE_DELAY_MILLIS)
        }
    }

    init {
        registerLocaleChangeReceiver()

        navigator.addNativeNavigatorRecreationObserver {
            updateUserLanguages()
        }
    }

    fun setOverrideLanguages(languages: List<String>?) {
        if (isDestroyed || overrideLanguages == languages) {
            return
        }
        overrideLanguages = languages
        if (languages == null) {
            registerLocaleChangeReceiver()
        } else {
            unregisterLocaleChangeReceiver()
            updateUserLanguages()
        }
    }

    fun destroy() {
        isDestroyed = true
        unregisterLocaleChangeReceiver()
    }

    private fun updateUserLanguages() {
        navigator.setUserLanguages(overrideLanguages ?: context.deviceLanguageTags)
    }

    private fun registerLocaleChangeReceiver() {
        if (isLocaleChangeReceiverRegistered || isDestroyed) {
            return
        }
        ContextCompat.registerReceiver(
            context,
            localeChangeReceiver,
            IntentFilter(Intent.ACTION_LOCALE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        isLocaleChangeReceiverRegistered = true
        updateUserLanguages()
    }

    private fun unregisterLocaleChangeReceiver() {
        if (!isLocaleChangeReceiverRegistered) {
            return
        }
        context.unregisterReceiver(localeChangeReceiver)
        isLocaleChangeReceiverRegistered = false
    }

    companion object {

        private const val LOCALE_UPDATE_DELAY_MILLIS = 100L

        fun create(
            context: Context,
            navigator: MapboxNativeNavigator,
            handler: Handler = Handler(Looper.getMainLooper()),
        ) = SystemLocaleWatcher(context, navigator, handler)

        private val Context.deviceLanguageTags: List<String>
            get() = inferDeviceLocales().map { it.toLanguageTag() }
    }
}

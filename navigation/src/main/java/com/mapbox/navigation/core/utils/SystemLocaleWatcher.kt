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
    navigator: MapboxNativeNavigator,
    private val handler: Handler,
) {

    private val localeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // When we receive this event, the system's configuration update might not have been
            // fully propagated, so we introduce a small delay before we update language
            handler.postDelayed({
                navigator.setUserLanguages(context.deviceLanguageTags)
            }, LOCALE_UPDATE_DELAY_MILLIS,)
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            localeChangeReceiver,
            IntentFilter(Intent.ACTION_LOCALE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        navigator.addNativeNavigatorRecreationObserver {
            navigator.setUserLanguages(context.deviceLanguageTags)
        }
        navigator.setUserLanguages(context.deviceLanguageTags)
    }

    fun destroy() {
        context.unregisterReceiver(localeChangeReceiver)
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

@file:JvmName("ContextEx")

package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Returns the device language to default to if no locale was specified
 *
 * @return language of device
 */
fun Context.inferDeviceLanguage(): String = inferDeviceLocale().language

/**
 * Returns the device locale for which to use as a default if no language is specified
 *
 * @return locale of device
 */
fun Context.inferDeviceLocale(): Locale =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = this.resources.configuration.locales
        if (!locales.isEmpty && locales[0] != null) {
            locales[0]
        } else {
            Locale.getDefault()
        }
    } else {
        this.resources.configuration.locale
    }

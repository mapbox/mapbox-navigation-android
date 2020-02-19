@file:JvmName("LocaleEx")

package com.mapbox.navigation.base.extensions

import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.VoiceUnit
import java.util.Locale

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
@VoiceUnit
fun Locale.getUnitTypeForLocale(): String =
    when (this.country) {
        "US", // US
        "LR", // Liberia
        "MM" -> // Burma
            IMPERIAL
        else ->
            METRIC
    }

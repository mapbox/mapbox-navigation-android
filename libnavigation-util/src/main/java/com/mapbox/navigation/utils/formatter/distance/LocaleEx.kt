@file:JvmName("LocaleEx")

package com.mapbox.navigation.model.formatter.distance

import com.mapbox.navigation.base.model.route.RouteConstants.IMPERIAL
import com.mapbox.navigation.base.model.route.RouteConstants.METRIC
import com.mapbox.navigation.base.typedef.VoiceUnitCriteria
import java.util.Locale

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
@VoiceUnitCriteria
fun Locale.getUnitTypeForLocale(): String =
    when (this.country) {
        "US", // US
        "LR", // Liberia
        "MM" -> // Burma
            IMPERIAL
        else ->
            METRIC
    }

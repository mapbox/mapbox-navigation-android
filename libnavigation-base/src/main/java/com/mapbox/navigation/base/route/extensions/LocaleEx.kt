@file:JvmName("LocaleEx")

package com.mapbox.navigation.base.route.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import java.util.Locale

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
@DirectionsCriteria.VoiceUnitCriteria
fun Locale.getUnitTypeForLocale(): String =
    when (this.country) {
        "US", // US
        "LR", // Liberia
        "MM" -> // Burma
            DirectionsCriteria.IMPERIAL
        else ->
            DirectionsCriteria.METRIC
    }

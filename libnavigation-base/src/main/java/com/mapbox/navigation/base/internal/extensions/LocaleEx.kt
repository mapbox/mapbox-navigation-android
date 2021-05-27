package com.mapbox.navigation.base.internal.extensions

import com.mapbox.navigation.base.formatter.UnitType
import java.util.Locale

/**
 * Returns the [UnitType] type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the default unit type for the locale.
 *
 * @receiver Locale for which to return the default unit type
 * @return [UnitType]
 */
fun Locale.getUnitTypeForLocale(): UnitType =
    when (this.country.uppercase(this)) {
        "US", // US
        "LR", // Liberia
        "MM" -> // Burma
            UnitType.IMPERIAL
        else ->
            UnitType.METRIC
    }

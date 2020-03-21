package com.mapbox.navigation.base.extensions

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
object LocaleEx {
    @JvmStatic
    @VoiceUnit
    fun Locale.getUnitTypeForLocale(): String =
            when (this.country.toUpperCase(this)) {
                "US", // US
                "LR", // Liberia
                "MM" -> // Burma
                    IMPERIAL
                else ->
                    METRIC
            }
    @JvmStatic
    fun getLocaleDirectionsRoute(directionsRoute: DirectionsRoute, context: Context): Locale {
        return if (directionsRoute.voiceLanguage() != null) {
            Locale(directionsRoute.voiceLanguage())
        } else {
            context.inferDeviceLocale()
        }
    }
}

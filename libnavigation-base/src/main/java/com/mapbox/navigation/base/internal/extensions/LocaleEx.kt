package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.internal.VoiceUnit
import java.util.Locale

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
object LocaleEx {

    /**
     * Returns the [VoiceUnit] type for the specified locale. Try to avoid using this unnecessarily because
     * all methods consuming unit type are able to handle the [VoiceUnit.UNDEFINED] type
     *
     * @receiver Locale for which to return the default unit type
     * @return String [VoiceUnit]
     */
    @JvmStatic
    @VoiceUnit.Type
    fun Locale.getUnitTypeForLocale(): String =
        when (this.country.toUpperCase(this)) {
            "US", // US
            "LR", // Liberia
            "MM" -> // Burma
                VoiceUnit.IMPERIAL
            else ->
                VoiceUnit.METRIC
        }

    /**
     * Provide [Locale] based on voice language of [DirectionsRoute] or default device's location if
     * non-specified
     */
    @JvmStatic
    fun getLocaleDirectionsRoute(directionsRoute: DirectionsRoute, context: Context): Locale {
        return if (directionsRoute.voiceLanguage() != null) {
            Locale(directionsRoute.voiceLanguage())
        } else {
            context.inferDeviceLocale()
        }
    }
}

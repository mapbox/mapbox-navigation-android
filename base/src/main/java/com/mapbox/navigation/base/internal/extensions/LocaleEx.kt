package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.formatter.UnitType
import java.util.Locale

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
object LocaleEx {

    /**
     * Returns the [UnitType] type for the specified locale. Try to avoid using this unnecessarily because
     * all methods consuming unit type are able to handle the default unit type for the locale.
     *
     * @receiver Locale for which to return the default unit type
     * @return [UnitType]
     */
    @JvmStatic
    fun Locale.getUnitTypeForLocale(): UnitType =
        when (this.country.uppercase(this)) {
            "US", // US
            "LR", // Liberia
            "MM",
            -> // Burma
                UnitType.IMPERIAL
            else ->
                UnitType.METRIC
        }

    /**
     * Provide [Locale] based on voice language of [DirectionsRoute] or default device's location if
     * non-specified
     */
    @JvmStatic
    fun getLocaleDirectionsRoute(directionsRoute: DirectionsRoute, context: Context): Locale {
        return getVoiceLocale(directionsRoute.voiceLanguage(), context)
    }

    /**
     * Provide [Locale] based on voice language or default device's location if non-specified
     */
    @JvmStatic
    fun getVoiceLocale(voiceLanguage: String?, context: Context): Locale {
        return voiceLanguage
            ?.let { Locale.forLanguageTag(it) }
            ?: context.inferDeviceLocale()
    }
}

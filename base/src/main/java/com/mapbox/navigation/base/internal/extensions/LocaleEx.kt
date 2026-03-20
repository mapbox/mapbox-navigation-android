package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.utils.internal.logE
import java.util.Locale
import java.util.MissingResourceException

/**
 * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
 * all methods consuming unit type are able to handle the NONE_SPECIFIED type
 *
 * @return unit type for specified locale
 */
object LocaleEx {

    private const val TAG = "LocaleExt"

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

    /**
     * Returns the ISO 639-2/T three-letter language code for this locale, or `null` if the locale
     * has an unknown or invalid language code that causes [Locale.getISO3Language] to throw
     * a [MissingResourceException] (e.g. the synthetic language tag `"xx"`).
     *
     * @receiver Locale whose ISO 3-letter language code is requested
     * @return three-letter language code, or `null` on failure
     */
    @JvmStatic
    fun Locale.getISO3LanguageOrNull(): String? {
        return try {
            this.isO3Language
        } catch (mre: MissingResourceException) {
            logE(TAG) { "getISO3Language failed: ${mre.message}" }
            null
        }
    }

    /**
     * Returns the ISO 639-2/T three-letter language code for this locale, or [defaultLanguage]
     * if the code cannot be resolved (e.g. unknown/synthetic language tags like `"xx"`).
     *
     * @receiver Locale whose ISO 3-letter language code is requested
     * @param defaultLanguage value to return when the ISO 3-letter code is unavailable;
     * defaults to an empty string
     * @return three-letter language code, or [defaultLanguage] on failure
     */
    @JvmStatic
    fun Locale.getISO3LanguageOrDefault(defaultLanguage: String = ""): String =
        this.getISO3LanguageOrNull() ?: defaultLanguage
}

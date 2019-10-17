package com.mapbox.services.android.navigation.v5.utils

import android.content.Context
import android.os.Build
import com.mapbox.api.directions.v5.DirectionsCriteria
import java.util.Locale

class LocaleUtils {

    /**
     * Returns the unit type for the specified locale. Try to avoid using this unnecessarily because
     * all methods consuming unit type are able to handle the NONE_SPECIFIED type
     *
     * @param locale for which to return the default unit type
     * @return unit type for specified locale
     */
    @DirectionsCriteria.VoiceUnitCriteria
    fun getUnitTypeForLocale(locale: Locale): String {
        return when (locale.country) {
            "US", // US
                "LR", // Liberia
                "MM" // Burma
            -> DirectionsCriteria.IMPERIAL
            else -> DirectionsCriteria.METRIC
        }
    }

    /**
     * Returns the device language to default to if no locale was specified
     *
     * @param context to check configuration
     * @return language of device
     */
    fun inferDeviceLanguage(context: Context): String {
        return inferDeviceLocale(context).language
    }

    /**
     * Returns the device locale for which to use as a default if no language is specified
     *
     * @param context to check configuration
     * @return locale of device
     */
    fun inferDeviceLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
    }

    /**
     * Returns the locale passed in if it is not null, otherwise returns the device locale
     *
     * @param context to get device locale
     * @param language to check if it is null
     * @return a non-null locale, either the one passed in, or the device locale
     */
    fun getNonEmptyLanguage(context: Context, language: String?): String {
        return language ?: inferDeviceLanguage(context)
    }

    /**
     * Returns the unit type for the device locale
     *
     * @param context from which to get the configuration
     * @return the default unit type for the device
     */
    fun getUnitTypeForDeviceLocale(context: Context): String {
        return getUnitTypeForLocale(inferDeviceLocale(context))
    }

    /**
     * Returns the unitType passed in if it is not null, otherwise returns the a unitType
     * based on the device Locale.
     *
     * @param context to get device locale
     * @param unitType to check if it is null
     * @return a non-null unitType, either the one passed in, or based on the device locale
     */
    fun retrieveNonNullUnitType(context: Context, unitType: String?): String {
        return unitType ?: getUnitTypeForDeviceLocale(context)
    }
}

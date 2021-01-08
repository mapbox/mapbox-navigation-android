package com.mapbox.navigation.ui.base.model.tripprogress

import android.content.Context
import android.text.SpannableString
import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.core.Rounding
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import java.util.Locale

/**
 * Formats trip related data for displaying in the UI
 *
 * @param applicationContext an application context instance
 * @param unitType a value indicating the distance units like mi for miles or km for kilometers. If
 * not provided the value is derived from the [Locale].
 * @param locale an optional [Locale], if not provided the local will be derived from the context
 * @param roundingIncrement minimal value that distance might be stripped
 */
class DistanceRemainingFormatter(
    context: Context,
    unitType: String = VoiceUnit.UNDEFINED,
    locale: Locale? = null,
    @Rounding.Increment roundingIncrement: Int = Rounding.INCREMENT_FIFTY
) : ValueFormatter<TripProgressUpdate, SpannableString> {

    private val appContext: Context = context.applicationContext

    private val mapboxDistanceFormatter by lazy {
        val localeToUse: Locale = locale ?: appContext.inferDeviceLocale()
        MapboxDistanceFormatter.Builder(appContext)
            .locale(localeToUse)
            .roundingIncrement(roundingIncrement)
            .unitType(unitType)
            .build()
    }

    /**
     * Formats the data in the [TripProgressUpdate] for displaying the route distance remaining
     * in the UI
     *
     * @param update a [TripProgressUpdate]
     * @return a [SpannableString] representing the route distance remaining
     */
    override fun format(update: TripProgressUpdate): SpannableString {
        return mapboxDistanceFormatter.formatDistance(update.distanceRemaining)
    }
}

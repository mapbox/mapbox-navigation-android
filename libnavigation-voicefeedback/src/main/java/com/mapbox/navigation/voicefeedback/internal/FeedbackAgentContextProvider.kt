package com.mapbox.navigation.voicefeedback.internal

import android.os.Build
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Provides a mechanism for retrieving the current Feedback Agent context.
 *
 * Implementations of this interface supply the context data required for Feedback Agent interactions.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal interface FeedbackAgentContextProvider {

    /**
     * Retrieves the current Feedback Agent context.
     *
     * @return The current [FeedbackAgentContextDTO].
     */
    fun getContext(): FeedbackAgentContextDTO
}

private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

@ExperimentalPreviewMapboxNavigationAPI
internal class DefaultContextProvider(
    private val locale: Locale,
    private val locationProvider: () -> LocationMatcherResult?,
) : FeedbackAgentContextProvider {
    private val formatter by lazy {
        SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
    }

    override fun getContext(): FeedbackAgentContextDTO {
        return FeedbackAgentContextDTO(
            userContext = getUserContext(),
            appContext = getAppContext(),
        )
    }

    private fun getUserContext(): FeedbackAgentUserContextDTO {
        val location = locationProvider()?.enhancedLocation
            ?: return FeedbackAgentUserContextDTO("", "", null, "")

        return FeedbackAgentUserContextDTO(
            lat = location.latitude.toString(),
            lon = location.longitude.toString(),
            heading = location.bearing.toString(),
            placeName = "",
        )
    }

    private fun getAppContext(): FeedbackAgentAppContextDTO {
        val (temperatureUnits, distanceUnits) = when (locale.getUnitTypeForLocale()) {
            UnitType.IMPERIAL -> "Fahrenheit" to "mi"
            UnitType.METRIC -> "Celsius" to "km"
        }

        return FeedbackAgentAppContextDTO(
            locale = locale.toLanguageTag(),
            temperatureUnits = temperatureUnits,
            distanceUnits = distanceUnits,
            clientTime = getClientTime(),
        )
    }

    private fun getClientTime(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } else {
            val time = Calendar.getInstance().time
            return formatter.format(time)
        }
    }
}

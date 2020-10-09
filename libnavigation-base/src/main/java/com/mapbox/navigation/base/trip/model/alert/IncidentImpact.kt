package com.mapbox.navigation.base.trip.model.alert

import androidx.annotation.StringDef

/**
 * Rate impacts of [IncidentAlert].
 */
object IncidentImpact {

    /**
     * Incident Impact unknown.
     */
    const val UNKNOWN = "unknown"

    /**
     * Incident Impact critical.
     */
    const val CRITICAL = "critical"

    /**
     * Incident Impact major.
     */
    const val MAJOR = "major"

    /**
     * Incident Impact minor.
     */
    const val MINOR = "minor"

    /**
     * Incident Impact low.
     */
    const val LOW = "low"

    /**
     * Incident Alert impact.
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        UNKNOWN,
        CRITICAL,
        MAJOR,
        MINOR,
        LOW
    )
    annotation class Impact
}

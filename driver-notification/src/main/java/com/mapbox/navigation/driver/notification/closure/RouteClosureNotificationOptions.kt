package com.mapbox.navigation.driver.notification.closure

/**
 * Options for configuring [RouteClosureNotificationProvider].
 *
 * @see [RouteClosureNotificationProvider] for the provider that uses these options.
 */
class RouteClosureNotificationOptions internal constructor(

    /**
     * Distance threshold in metres that separates the "monitoring" phase (far closure,
     * informational banner) from the "find an alternative" phase (close closure, alternative
     * route proposal).
     *
     * Closures detected beyond this distance trigger a [RouteClosureMonitoringNotification].
     * Closures detected at or within this distance trigger a [RouteClosureAlternativeNotification]
     * when an alternative route becomes available.
     *
     * Default is [DEFAULT_ALTERNATIVE_TRIGGER_THRESHOLD_METERS] (250 km).
     */
    val alternativeTriggerThresholdMeters: Double,
) {

    /**
     * Creates a new [Builder] pre-populated with this instance's values.
     */
    fun toBuilder(): Builder = Builder()
        .alternativeTriggerThresholdMeters(alternativeTriggerThresholdMeters)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RouteClosureNotificationOptions
        return alternativeTriggerThresholdMeters == other.alternativeTriggerThresholdMeters
    }

    override fun hashCode(): Int = alternativeTriggerThresholdMeters.hashCode()

    override fun toString(): String =
        "RouteClosureNotificationOptions(" +
            "alternativeTriggerThresholdMeters=$alternativeTriggerThresholdMeters" +
            ")"

    /**
     * Builder for creating instances of [RouteClosureNotificationOptions].
     */
    class Builder {

        private var alternativeTriggerThresholdMeters: Double =
            DEFAULT_ALTERNATIVE_TRIGGER_THRESHOLD_METERS

        /**
         * Sets the distance threshold separating the monitoring phase from the
         * alternative-search phase.
         *
         * Default is [DEFAULT_ALTERNATIVE_TRIGGER_THRESHOLD_METERS] (250 km).
         */
        fun alternativeTriggerThresholdMeters(value: Double): Builder = apply {
            alternativeTriggerThresholdMeters = value
        }

        /**
         * Builds a new instance of [RouteClosureNotificationOptions].
         */
        fun build(): RouteClosureNotificationOptions =
            RouteClosureNotificationOptions(
                alternativeTriggerThresholdMeters = alternativeTriggerThresholdMeters,
            )
    }

    companion object {
        const val DEFAULT_ALTERNATIVE_TRIGGER_THRESHOLD_METERS = 250_000.0
    }
}

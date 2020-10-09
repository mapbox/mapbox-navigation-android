package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.geojson.Point

/**
 * Incident alert provides information about incidents on a way like *congestion*, *mass transit*,
 * etc. More types of incidents see [IncidentType].
 *
 * @param info incident alert info.
 */
class IncidentAlert private constructor(
    coordinate: Point,
    distance: Double,
    alertGeometry: RouteAlertGeometry?,
    val info: IncidentInfo?
) : RouteAlert(RouteAlertType.Incident, coordinate, distance, alertGeometry) {

    /**
     * Transform this object into a builder to mutate the values.
     */
    fun toBuilder(): Builder = Builder(coordinate, distance)
        .alertGeometry(alertGeometry)
        .info(info)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as IncidentAlert

        if (info != other.info) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + info.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "IncidentAlert(" +
            "info=$info" +
            "), ${super.toString()}"
    }

    /**
     * Use to create a new instance.
     *
     * @see IncidentAlert
     */
    class Builder(
        private val coordinate: Point,
        private val distance: Double
    ) {

        private var alertGeometry: RouteAlertGeometry? = null
        private var info: IncidentInfo? = null

        /**
         * Add optional geometry if the alert has a length.
         */
        fun alertGeometry(alertGeometry: RouteAlertGeometry?): Builder = apply {
            this.alertGeometry = alertGeometry
        }

        /**
         * Incident alert info.
         */
        fun info(incidentInfo: IncidentInfo?): Builder = apply {
            this.info = incidentInfo
        }

        /**
         * Build the object instance.
         */
        fun build(): IncidentAlert =
            IncidentAlert(
                coordinate,
                distance,
                alertGeometry,
                info
            )
    }
}

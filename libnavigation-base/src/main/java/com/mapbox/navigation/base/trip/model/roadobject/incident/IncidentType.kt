package com.mapbox.navigation.base.trip.model.roadobject.incident

import androidx.annotation.IntDef

/**
 * Types of [Incident].
 */
object IncidentType {

    /**
     * Incident Alert Type is unknown.
     */
    const val UNKNOWN = 40

    /**
     * Incident Alert Type accident.
     */
    const val ACCIDENT = 41

    /**
     * Incident Alert Type congestion.
     */
    const val CONGESTION = 42

    /**
     * Incident Alert Type construction.
     */
    const val CONSTRUCTION = 43

    /**
     * Incident Alert Type disabled vehicle.
     */
    const val DISABLED_VEHICLE = 44

    /**
     * Incident Alert Type lane restriction.
     */
    const val LANE_RESTRICTION = 45

    /**
     * Incident Alert Type mass transit.
     */
    const val MASS_TRANSIT = 46

    /**
     * Incident Alert Type miscellaneous.
     */
    const val MISCELLANEOUS = 47

    /**
     * Incident Alert Type other news.
     */
    const val OTHER_NEWS = 48

    /**
     * Incident Alert Type planned event.
     */
    const val PLANNED_EVENT = 49

    /**
     * Incident Alert Type road closure.
     */
    const val ROAD_CLOSURE = 50

    /**
     * Incident Alert Type road hazard.
     */
    const val ROAD_HAZARD = 51

    /**
     * Incident Alert Type weather.
     */
    const val WEATHER = 52

    /**
     * Incident type.
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        UNKNOWN,
        ACCIDENT,
        CONGESTION,
        CONSTRUCTION,
        DISABLED_VEHICLE,
        LANE_RESTRICTION,
        MASS_TRANSIT,
        MISCELLANEOUS,
        OTHER_NEWS,
        PLANNED_EVENT,
        ROAD_CLOSURE,
        ROAD_HAZARD,
        WEATHER,
    )
    annotation class Type
}

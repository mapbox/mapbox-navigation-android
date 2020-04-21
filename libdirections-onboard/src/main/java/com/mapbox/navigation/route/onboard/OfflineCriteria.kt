package com.mapbox.navigation.route.onboard

/**
 * Offline Criteria. Used in Direction API
 */
internal object OfflineCriteria {

    /**
     * BicycleType parameter in the Directions API.
     *
     * @property type
     */
    enum class BicycleType(val type: String) {
        /**
         * Bicycle type for road bike.
         */
        ROAD("Road"),

        /**
         * Bicycle type for hybrid bike.
         */
        HYBRID("Hybrid"),

        /**
         * Bicycle type for city bike.
         */
        CITY("City"),

        /**
         * Bicycle type for cross bike.
         */
        CROSS("Cross"),

        /**
         * Bicycle type for mountain bike.
         */
        MOUNTAIN("Mountain");
    }

    /**
     * WaypointType parameter in the Directions API.
     *
     * @property type
     */
    enum class WaypointType(val type: String) {
        /**
         * Break waypoint type.
         */
        BREAK("break"),

        /**
         * Through waypoint type.
         */
        THROUGH("through")
    }
}

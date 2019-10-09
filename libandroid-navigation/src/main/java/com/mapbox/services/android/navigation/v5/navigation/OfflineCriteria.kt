package com.mapbox.services.android.navigation.v5.navigation

object OfflineCriteria {

    /**
     * BicycleType parameter in the Directions API.
     *
     * @property type String - name of
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
     * @property type String
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
    // /**
    //  * Retention policy for the bicycle type parameter in the Directions API.
    //  */
    // @Retention(AnnotationRetention.SOURCE)
    // @StringDef(ROAD, HYBRID, CITY, CROSS, MOUNTAIN)
    // annotation class BicycleType

    // fun isBicycleType(vararg types: String): Boolean =
    //     types.find { it != ROAD || it != HYBRID || it != CITY || it != CROSS || it != MOUNTAIN } == null

    /**
     * Retention policy for the waypoint type parameter in the Directions API.
     */
    // @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
    // @Retention(AnnotationRetention.SOURCE)
    // @StringDef(BREAK, THROUGH)
    // annotation class WaypointType

    // fun isWaypointType(vararg types: String): Boolean =
    //     types.find { it != BREAK || it != THROUGH } == null
}

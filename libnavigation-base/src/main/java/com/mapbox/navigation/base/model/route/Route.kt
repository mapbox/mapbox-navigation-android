package com.mapbox.navigation.base.model.route

data class Route internal constructor(
    private var distance: Double? = null,
    private var duration: Double? = null,
    private var legs: List<RouteLeg>? = null,
    private var routeOptions: RouteOptions? = null
) {

    /**
     * The distance traveled from origin to destination.
     *
     * @return a double number with unit meters
     * @since 1.0.0
     */
    fun distance(): Double? = distance

    /**
     * The estimated travel time from origin to destination.
     *
     * @return a double number with unit seconds
     * @since 1.0.0
     */
    fun duration(): Double? = duration

    /**
     * A Leg is a route between only two waypoints.
     *
     * @return list of [RouteLeg] objects
     * @since 1.0.0
     */
    fun legs(): List<RouteLeg>? = legs

    /**
     * Holds onto the parameter information used when making the directions request. Useful for
     * re-requesting a directions route using the same information previously used.
     *
     * @return a [RouteOptions]s object which holds onto critical information from the request
     * that cannot be derived directly from the directions route
     * @since 3.0.0
     */
    fun routeOptions(): RouteOptions? = routeOptions

    class Builder {
        var distance: Double? = null
        var duration: Double? = null
        var legs: List<RouteLeg>? = null
        var routeOptions: RouteOptions? = null

        fun distance(distance: Double) =
                apply { this.distance = distance }

        fun duration(duration: Double) =
                apply { this.duration = duration }

        fun legs(legs: List<RouteLeg>) =
                apply { this.legs = legs }

        fun routeOptions(routeOptions: RouteOptions) =
                apply { this.routeOptions = routeOptions }

        fun build(): Route {
            return Route(distance, duration, legs, routeOptions)
        }
    }
}

package com.mapbox.navigation.base.model.route

data class Route internal constructor(
    private var distance: Double? = null,
    private var duration: Double? = null,
    private var legs: List<RouteLeg>? = null,
    private var routeOptions: RouteOptions? = null,
    private var builder: Builder
) {

    fun distance(): Double? = distance

    fun duration(): Double? = duration

    fun legs(): List<RouteLeg>? = legs

    fun routeOptions(): RouteOptions? = routeOptions

    fun builder(): Builder = builder

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
            return Route(distance, duration, legs, routeOptions, this)
        }
    }
}

package com.mapbox.navigation.core

internal sealed class SetRoutes {

    internal object CleanUp : SetRoutes()

    internal data class NewRoutes(
        val legIndex: Int,
    ) : SetRoutes()

    internal object Reroute : SetRoutes()

    internal data class Alternatives(
        val legIndex: Int,
    ) : SetRoutes()

    internal data class RefreshRoutes(
        val routeProgressData: RouteProgressData
    ) : SetRoutes()
}

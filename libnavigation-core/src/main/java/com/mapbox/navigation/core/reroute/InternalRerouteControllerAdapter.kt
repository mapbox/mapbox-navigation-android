package com.mapbox.navigation.core.reroute

internal class InternalRerouteControllerAdapter(
    private val originalController: NavigationRerouteController
) : InternalRerouteController, NavigationRerouteController by originalController

package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.RouteLeg

internal class TrafficCongestionProvider {
    private var trafficFun: (RouteLeg) -> List<String>? = { null }

    fun updateTrafficFunction(trafficFun: (RouteLeg) -> List<String>?) {
        this.trafficFun = trafficFun
    }

    fun getTrafficFunction(): (RouteLeg) -> List<String>? {
        return trafficFun
    }
}

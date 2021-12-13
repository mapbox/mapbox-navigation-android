package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldToDownload

fun MapboxRouteShieldApi.getRouteShieldsFrom(
    maneuvers: List<Maneuver>,
    callback: RoadShieldCallback
) {
    val primaryMap = hashMapOf<String, String?>()
    val secondaryMap = hashMapOf<String, String?>()
    val subMap = hashMapOf<String, String?>()
    val routeShieldToDownload = mutableListOf<RouteShieldToDownload.MapboxLegacy>()
    maneuvers.forEach { maneuver ->
        val primaryManeuverComponents = maneuver.primary.componentList
        primaryManeuverComponents.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxLegacy(url = component.node.shieldUrl)
                routeShieldToDownload.add(toDownload)
                primaryMap[maneuver.primary.id] = component.node.shieldUrl
            }
        }
        val secondaryManeuverComponents = maneuver.secondary?.componentList
        secondaryManeuverComponents?.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxLegacy(url = component.node.shieldUrl)
                routeShieldToDownload.add(toDownload)
                secondaryMap[maneuver.secondary.id] = component.node.shieldUrl
            }
        }
        val subManeuverComponents = maneuver.sub?.componentList
        subManeuverComponents?.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxLegacy(url = component.node.shieldUrl)
                routeShieldToDownload.add(toDownload)
                subMap[maneuver.sub.id] = component.node.shieldUrl
            }
        }
    }
    getLegacyRouteShields(routeShieldToDownload) { result ->
        val shieldMap = hashMapOf<String, RoadShield>()
        val errorMap = hashMapOf<String, RoadShieldError>()
        result.forEach { expected ->
            if (expected.isValue) {
                val routeShield = expected.value!!.shield as RouteShield.MapboxLegacyShield
                val primaryKeys = primaryMap.filter { routeShield.url == it.value }.keys
                val secondaryKeys = secondaryMap.filter { routeShield.url == it.value }.keys
                val subKeys = subMap.filter { routeShield.url == it.value }.keys
                val id = when {
                    primaryKeys.isNotEmpty() -> { primaryKeys.first() }
                    secondaryKeys.isNotEmpty() -> { secondaryKeys.first() }
                    subKeys.isNotEmpty() -> { subKeys.first() }
                    else -> { "" }
                }
                val roadShield = RoadShield(shieldUrl = routeShield.url, shieldIcon = routeShield.shield)
                shieldMap[id] = roadShield
            } else if (expected.isError) {
                val routeShieldError = expected.error!!
                val primaryKeys = primaryMap.filter { routeShieldError.url == it.value }.keys
                val secondaryKeys = secondaryMap.filter { routeShieldError.url == it.value }.keys
                val subKeys = subMap.filter { routeShieldError.url == it.value }.keys
                val id = when {
                    primaryKeys.isNotEmpty() -> { primaryKeys.first() }
                    secondaryKeys.isNotEmpty() -> { secondaryKeys.first() }
                    subKeys.isNotEmpty() -> { subKeys.first() }
                    else -> { "" }
                }
                // TODO: How to fix this to avoid breaking SEMVER
                val roadShieldError = RoadShieldError(url = routeShieldError.url, message = routeShieldError.errorMessage)
                errorMap[id] = roadShieldError
            }
        }
        callback.onRoadShields(maneuvers = maneuvers, shields = shieldMap, errors = errorMap)
    }
}

fun MapboxRouteShieldApi.getRouteShieldsFrom(
    userId: String,
    styleId: String,
    accessToken: String,
    fallbackToLegacy: Boolean,
    maneuvers: List<Maneuver>,
    callback: RoadShieldCallback
) {
    val primaryMap = hashMapOf<String, String?>()
    val secondaryMap = hashMapOf<String, String?>()
    val subMap = hashMapOf<String, String?>()
    val routeShieldToDownload = mutableListOf<RouteShieldToDownload.MapboxDesign>()
    maneuvers.forEach { maneuver ->
        val primaryManeuverComponents = maneuver.primary.componentList
        primaryManeuverComponents.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxDesign(
                    mapboxShield = component.node.mapboxShield,
                    legacy = RouteShieldToDownload.MapboxLegacy(component.node.shieldUrl)
                )
                routeShieldToDownload.add(toDownload)
                primaryMap[maneuver.primary.id] = component.node.shieldUrl
            }
        }
        val secondaryManeuverComponents = maneuver.secondary?.componentList
        secondaryManeuverComponents?.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxDesign(
                    mapboxShield = component.node.mapboxShield,
                    legacy = RouteShieldToDownload.MapboxLegacy(component.node.shieldUrl)
                )
                routeShieldToDownload.add(toDownload)
                secondaryMap[maneuver.secondary.id] = component.node.shieldUrl
            }
        }
        val subManeuverComponents = maneuver.sub?.componentList
        subManeuverComponents?.forEach { component ->
            if (component.node is RoadShieldComponentNode) {
                val toDownload = RouteShieldToDownload.MapboxDesign(
                    mapboxShield = component.node.mapboxShield,
                    legacy = RouteShieldToDownload.MapboxLegacy(component.node.shieldUrl)
                )
                routeShieldToDownload.add(toDownload)
                subMap[maneuver.sub.id] = component.node.shieldUrl
            }
        }
    }
    getMapboxDesignedRouteShields(
        userId = userId,
        styleId = styleId,
        accessToken = accessToken,
        fallbackToLegacy = fallbackToLegacy,
        shieldsToDownload = routeShieldToDownload
    ) { result ->
        val shieldMap = hashMapOf<String, RoadShield>()
        val errorMap = hashMapOf<String, RoadShieldError>()
        result.forEach { expected ->
            // convert RouteShieldCallback to RoadShieldCallback
            if (expected.isValue) {
                val routeShield = expected.value!!.shield as RouteShield.MapboxDesignedShield
                val primaryKeys = primaryMap.filter { routeShield.url == it.value }.keys
                val secondaryKeys = secondaryMap.filter { routeShield.url == it.value }.keys
                val subKeys = subMap.filter { routeShield.url == it.value }.keys
                val id = when {
                    primaryKeys.isNotEmpty() -> { primaryKeys.first() }
                    secondaryKeys.isNotEmpty() -> { secondaryKeys.first() }
                    subKeys.isNotEmpty() -> { subKeys.first() }
                    else -> { "" }
                }
                val roadShield = RoadShield(shieldUrl = routeShield.url, shieldIcon = routeShield.shield)
                shieldMap[id] = roadShield
            } else if (expected.isError) {
                val routeShieldError = expected.error!!
                val primaryKeys = primaryMap.filter { routeShieldError.url == it.value }.keys
                val secondaryKeys = secondaryMap.filter { routeShieldError.url == it.value }.keys
                val subKeys = subMap.filter { routeShieldError.url == it.value }.keys
                val id = when {
                    primaryKeys.isNotEmpty() -> { primaryKeys.first() }
                    secondaryKeys.isNotEmpty() -> { secondaryKeys.first() }
                    subKeys.isNotEmpty() -> { subKeys.first() }
                    else -> { "" }
                }
                // TODO: How to fix this to avoid breaking SEMVER
                val roadShieldError = RoadShieldError(url = routeShieldError.url, message = routeShieldError.errorMessage)
                errorMap[id] = roadShieldError
            }
        }
        callback.onRoadShields(maneuvers = maneuvers, shields = shieldMap, errors = errorMap)
    }
}

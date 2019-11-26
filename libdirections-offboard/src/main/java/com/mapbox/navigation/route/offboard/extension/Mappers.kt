package com.mapbox.navigation.route.offboard.extension

import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegsNavigation
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.WalkingOptionsNavigation

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance()!!,
    duration = duration()?.toLong()!!,
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()?.let { RouteLegsNavigation(it) },
    routeOptions = routeOptions()?.mapToRouteOptionsNavigation()
)

fun RouteOptions.mapToRouteOptionsNavigation() = RouteOptionsNavigation(
    alternatives = alternatives(),
    language = language(),
    radiuses = radiuses(),
    coordinates = coordinates(),
    profile = profile(),
    bearings = bearings(),
    continueStraight = continueStraight(),
    roundaboutExits = roundaboutExits(),
    geometries = geometries(),
    overview = overview(),
    steps = steps(),
    annotations = annotations(),
    exclude = exclude(),
    voiceInstructions = voiceInstructions(),
    bannerInstructions = bannerInstructions(),
    voiceUnits = voiceUnits(),
    requestUuid = requestUuid(),
    approaches = approaches(),
    waypointIndices = waypointIndices(),
    waypointNames = waypointNames(),
    waypointTargets = waypointTargets(),
    walkingOptions = walkingOptions()?.mapToWalkingOptionsNavigation()
)

fun WalkingOptions.mapToWalkingOptionsNavigation() = WalkingOptionsNavigation(
    walkingSpeed = walkingSpeed(),
    walkwayBias = walkwayBias()
)

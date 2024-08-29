package com.mapbox.navigation.core.mapmatching

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.MapMatchingMatch
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Interface definition for a callback associated with map matching request.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface MapMatchingAPICallback {
    /**
     * Called when routes are available.
     * @param result result of map matching
     */
    fun success(result: MapMatchingSuccessfulResult)

    /**
     * Called in case of error.
     * @param failure describes failure
     */
    fun failure(failure: MapMatchingFailure)

    /***
     * Called in case of map matching request cancellation
     */
    fun onCancel()
}

/**
 * Represents successful result of Map Matching API call.
 * @param matches Matched routes
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapMatchingSuccessfulResult internal constructor(
    val matches: List<MapMatchingMatch>,
) {

    /**
     * Routes which could be set to navigation.
     */
    val navigationRoutes: List<NavigationRoute> get() = matches.map { it.navigationRoute }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapMatchingSuccessfulResult

        return matches == other.matches
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return matches.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapMatchingSuccessfulResult(matches=$matches)"
    }
}

// TODO NAVAND-1736: add more info about failure
/***
 * Represents failure of Map Matching API call.
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapMatchingFailure internal constructor()

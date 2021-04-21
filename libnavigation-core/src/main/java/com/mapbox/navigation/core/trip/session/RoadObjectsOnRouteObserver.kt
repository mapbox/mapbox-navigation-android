package com.mapbox.navigation.core.trip.session

import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Observer that gets notified when route changes and new road objects on the route are available.
 *
 * @see MapboxNavigation.registerRoadObjectsOnRouteObserver
 */
interface RoadObjectsOnRouteObserver {

    /**
     * Invoked when the route has changed and new road objects are available,
     * or when the route is cleared.
     *
     * @param roadObjects road objects for the current route, or empty list if the route is cleared.
     */
    fun onNewRoadObjectsOnTheRoute(roadObjects: List<UpcomingRoadObject>)
}

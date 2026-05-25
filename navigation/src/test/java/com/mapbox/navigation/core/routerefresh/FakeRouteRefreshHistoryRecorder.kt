package com.mapbox.navigation.core.routerefresh

internal class FakeRouteRefreshHistoryRecorder : RouteRefreshHistoryRecorder {

    val recordedEvents = mutableListOf<RouteRefreshHistoryEvent>()

    override fun recordRouteRefreshEvent(event: RouteRefreshHistoryEvent) {
        recordedEvents.add(event)
    }

    inline fun <reified T : RouteRefreshHistoryEvent> eventsOf(): List<T> =
        recordedEvents.filterIsInstance<T>()
}

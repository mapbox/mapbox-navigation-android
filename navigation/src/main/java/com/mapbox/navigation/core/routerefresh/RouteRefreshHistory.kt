@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.routerefresh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.history.MapboxHistoryRecorder

internal interface RouteRefreshHistoryRecorder {
    fun recordRouteRefreshEvent(event: RouteRefreshHistoryEvent)
}

internal object NoOpRouteRefreshHistoryRecorder : RouteRefreshHistoryRecorder {
    override fun recordRouteRefreshEvent(event: RouteRefreshHistoryEvent) = Unit
}

internal sealed class RouteRefreshHistoryEvent {
    class ImmediateRouteRefresh : RouteRefreshHistoryEvent() {
        override fun toEventJson() = """{"type":"ImmediateRouteRefresh"}"""

        class Requested : PeriodicRouteRefresh() {
            override fun toEventJson() = """{"type":"ImmediateRouteRefresh.Requested"}"""
        }
    }
    sealed class PeriodicRouteRefresh : RouteRefreshHistoryEvent() {
        class Started : PeriodicRouteRefresh() {
            override fun toEventJson() = """{"type":"PeriodicRouteRefresh.Started"}"""
        }
        class Paused : PeriodicRouteRefresh() {
            override fun toEventJson() = """{"type":"PeriodicRouteRefresh.Paused"}"""
        }
        class RoutesToRefreshUpdated(val ids: List<String>) : PeriodicRouteRefresh() {
            override fun toEventJson(): String {
                val idsJson = ids.joinToString(",") { "\"$it\"" }
                return """{"type":"PeriodicRouteRefresh.RoutesToRefreshUpdated","ids":[$idsJson]}"""
            }
        }
        class RefreshAttemptScheduled(val resumeDelay: Boolean) : PeriodicRouteRefresh() {
            override fun toEventJson(): String {
                return """{"type":"PeriodicRouteRefresh.RefreshAttemptScheduled",""" +
                    """"resumeDelay":$resumeDelay}"""
            }
        }
    }
    class RouteRefreshStateUpdated(
        @RouteRefreshExtra.RouteRefreshState val state: String,
    ) : RouteRefreshHistoryEvent() {
        override fun toEventJson() =
            """{"type":"RouteRefreshStateUpdated","state":"$state"}"""
    }

    class Destroyed : RouteRefreshHistoryEvent() {
        override fun toEventJson() = """{"type":"Destroyed"}"""
    }

    abstract fun toEventJson(): String
}

internal class MapboxHistoryRecorderWrapper(
    private val historyRecorder: MapboxHistoryRecorder,
) : RouteRefreshHistoryRecorder {
    override fun recordRouteRefreshEvent(event: RouteRefreshHistoryEvent) {
        historyRecorder.pushHistory("android_route_refresh_events", event.toEventJson())
    }
}

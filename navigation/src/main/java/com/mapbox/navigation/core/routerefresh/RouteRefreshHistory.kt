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

internal sealed interface RouteRefreshHistoryEvent {
    fun toEventJson(): String

    sealed interface ImmediateRouteRefresh : RouteRefreshHistoryEvent {
        class Requested : ImmediateRouteRefresh {
            override fun toEventJson() = """{"type":"ImmediateRouteRefresh.Requested"}"""
        }
    }
    sealed interface PeriodicRouteRefresh : RouteRefreshHistoryEvent {
        class Resumed : PeriodicRouteRefresh {
            override fun toEventJson() = """{"type":"PeriodicRouteRefresh.Resumed"}"""
        }
        class Paused : PeriodicRouteRefresh {
            override fun toEventJson() = """{"type":"PeriodicRouteRefresh.Paused"}"""
        }
        class RoutesToRefreshUpdated(val ids: List<String>) : PeriodicRouteRefresh {
            override fun toEventJson(): String {
                val idsJson = ids.joinToString(",") { "\"$it\"" }
                return """{"type":"PeriodicRouteRefresh.RoutesToRefreshUpdated","ids":[$idsJson]}"""
            }
        }
        class RefreshAttemptPosted(
            val resumePreviousAttemptDelay: Boolean,
            val isPaused: Boolean,
        ) : PeriodicRouteRefresh {
            override fun toEventJson(): String {
                return """{
                    "type":"PeriodicRouteRefresh.RefreshAttemptPosted",
                    "resumePreviousAttemptDelay":$resumePreviousAttemptDelay,
                    "isPaused": $isPaused
                    }
                """.trimIndent()
            }
        }
    }
    class RouteRefreshStateUpdated(
        @RouteRefreshExtra.RouteRefreshState val state: String,
    ) : RouteRefreshHistoryEvent {
        override fun toEventJson() =
            """{"type":"RouteRefreshStateUpdated","state":"$state"}"""
    }

    class Destroyed : RouteRefreshHistoryEvent {
        override fun toEventJson() = """{"type":"Destroyed"}"""
    }
}

internal class MapboxHistoryRecorderWrapper(
    private val historyRecorder: MapboxHistoryRecorder,
) : RouteRefreshHistoryRecorder {
    override fun recordRouteRefreshEvent(event: RouteRefreshHistoryEvent) {
        historyRecorder.pushHistory("android_route_refresh_events", event.toEventJson())
    }
}

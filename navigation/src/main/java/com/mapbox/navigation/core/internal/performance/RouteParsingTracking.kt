package com.mapbox.navigation.core.internal.performance

import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.utils.internal.Time

internal interface RouteParsingTracking {
    fun routeResponseIsParsed(metadata: RoutesResponse.Metadata)
}

internal class RouteParsingHistoryTracker(
    private val historyRecorder: MapboxHistoryRecorder,
) : RouteParsingTracking {
    override fun routeResponseIsParsed(metadata: RoutesResponse.Metadata) {
        with(metadata) {
            val json =
                """
                {
                "response_wait_duration": $responseWaitMillis,
                "response_parse_duration": $responseParseMillis,
                "response_parse_thread": "$responseParseThread",
                "native_wait_duration": $nativeWaitMillis,
                "native_parse_duration": $nativeParseMillis,
                "route_options_wait_duration": $routeOptionsWaitMillis,
                "route_options_parse_duration": $routeOptionsParseMillis,
                "main_thread_wait_duration": ${Time.SystemClockImpl.millis() - createdAtElapsedMillis}
                }
                """.trimIndent()
            historyRecorder.pushHistory("directions_response_parsing", json)
        }
    }
}

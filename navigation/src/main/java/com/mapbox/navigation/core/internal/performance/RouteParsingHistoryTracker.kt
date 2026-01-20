package com.mapbox.navigation.core.internal.performance

import com.mapbox.navigation.base.internal.route.RoutesResponse
import com.mapbox.navigation.base.internal.route.parsing.RouteParsingTracking
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.utils.internal.Time

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
                "main_thread_wait_duration": ${Time.SystemClockImpl.millis() - createdAtElapsedMillis}
                }
                """.trimIndent()
            historyRecorder.pushHistory("directions_response_parsing", json)
        }
    }
}

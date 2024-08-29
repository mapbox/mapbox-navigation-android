package com.mapbox.navigation.metrics.internal

import com.mapbox.common.TelemetryService

object TelemetryServiceProvider {

    fun provideTelemetryService(): TelemetryService = TelemetryService.getOrCreate()
}

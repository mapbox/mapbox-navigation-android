package com.mapbox.navigation.base.trip.model.alert

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class UpcomingRouteAlertTest : BuilderTest<UpcomingRouteAlert, UpcomingRouteAlert.Builder>() {
    override fun getImplementationClass() = UpcomingRouteAlert::class

    override fun getFilledUpBuilder() = UpcomingRouteAlert.Builder(mockk(relaxed = true), 10.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}

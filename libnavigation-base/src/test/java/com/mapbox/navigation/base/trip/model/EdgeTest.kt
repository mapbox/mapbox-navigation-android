package com.mapbox.navigation.base.trip.model

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class EdgeTest : BuilderTest<Edge, Edge.Builder>() {

    override fun getImplementationClass() = Edge::class

    override fun getFilledUpBuilder() = Edge.Builder()
        .id(3)
        .level(2.toByte())
        .probability(0.5)
        .heading(180.0)
        .length(100.0)
        .out(mockk(relaxed = true))
        .frc("MOTORWAY")
        .wayId("way id")
        .positiveDirection(false)
        .speed(40.0)
        .ramp(true)
        .motorway(true)
        .bridge(true)
        .tunnel(true)
        .toll(true)
        .names(mockk(relaxed = true))
        .curvature(8.toByte())
        .geometry(mockk())
        .speedLimit(30.0)
        .laneCount(3)
        .meanElevation(5.0)
        .countryCode("US")
        .stateCode("MD")

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}

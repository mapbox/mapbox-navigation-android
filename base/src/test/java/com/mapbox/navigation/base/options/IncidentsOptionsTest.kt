package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class IncidentsOptionsTest : BuilderTest<IncidentsOptions, IncidentsOptions.Builder>() {

    override fun getImplementationClass() = IncidentsOptions::class

    override fun getFilledUpBuilder() = IncidentsOptions.Builder()
        .graph("graph")
        .apiUrl("apiUrl")

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}

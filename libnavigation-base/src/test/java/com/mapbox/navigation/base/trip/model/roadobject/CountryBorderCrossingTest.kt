package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.roadobject.border.CountryBorderCrossing
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryBorderCrossingTest :
    BuilderTest<CountryBorderCrossing, CountryBorderCrossing.Builder>() {

    override fun getImplementationClass() = CountryBorderCrossing::class

    override fun getFilledUpBuilder() = CountryBorderCrossing.Builder(
        mockk(relaxed = true)
    )
        .distanceFromStartOfRoute(1.0)
        .countryBorderCrossingInfo(mockk(relaxed = true))

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val borderCrossing = CountryBorderCrossing.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, borderCrossing.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val borderCrossing = CountryBorderCrossing.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, borderCrossing.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val borderCrossing = CountryBorderCrossing.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, borderCrossing.distanceFromStartOfRoute)
    }
}

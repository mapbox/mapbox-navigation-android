package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollection
import com.mapbox.navigation.base.trip.model.roadobject.tollcollection.TollCollectionType
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TollCollectionTest : BuilderTest<TollCollection, TollCollection.Builder>() {
    override fun getImplementationClass() = TollCollection::class

    override fun getFilledUpBuilder() = TollCollection.Builder(
        mockk(relaxed = true)
    ).distanceFromStartOfRoute(123.0)
        .tollCollectionType(TollCollectionType.TollBooth)

    @Test
    override fun trigger() {
        // see docs
    }

    @Test
    fun `distanceFromStartOfRoute is null if negative value passed`() {
        val tollCollection = TollCollection.Builder(mockk())
            .distanceFromStartOfRoute(-1.0)
            .build()

        assertEquals(null, tollCollection.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute is null if null passed`() {
        val tollCollection = TollCollection.Builder(mockk())
            .distanceFromStartOfRoute(null)
            .build()

        assertEquals(null, tollCollection.distanceFromStartOfRoute)
    }

    @Test
    fun `distanceFromStartOfRoute not null if positive value passed`() {
        val tollCollection = TollCollection.Builder(mockk())
            .distanceFromStartOfRoute(1.0)
            .build()

        assertEquals(1.0, tollCollection.distanceFromStartOfRoute)
    }
}

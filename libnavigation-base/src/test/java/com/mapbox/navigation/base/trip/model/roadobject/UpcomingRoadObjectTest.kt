package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class UpcomingRoadObjectTest : BuilderTest<UpcomingRoadObject, UpcomingRoadObject.Builder>() {
    override fun getImplementationClass() = UpcomingRoadObject::class

    override fun getFilledUpBuilder() = UpcomingRoadObject.Builder(mockk(relaxed = true), 10.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}

package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class RestStopFromRoadObjectTest :
    BuilderTest<RestStopFromRoadObject, RestStopFromRoadObject.Builder>() {

    override fun getImplementationClass(): KClass<RestStopFromRoadObject> {
        return RestStopFromRoadObject::class
    }

    override fun getFilledUpBuilder(): RestStopFromRoadObject.Builder {
        return RestStopFromRoadObject
            .Builder(mockk())
            .distanceToStart(123.0)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}

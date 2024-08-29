package com.mapbox.navigation.base.trip.model.roadobject.ic

import com.mapbox.navigation.base.trip.model.roadobject.LocalizedString
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class InterchangeTest {

    @Test
    fun `type is IC`() {
        val actual = Interchange(
            "id",
            listOf(LocalizedString("en", "name")),
            1.0,
            RoadObjectProvider.MAPBOX,
            true,
            mockk(),
        )

        assertEquals(RoadObjectType.IC, actual.objectType)
    }
}

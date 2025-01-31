package com.mapbox.navigation.base.trip.model.roadobject.jct

import com.mapbox.navigation.base.trip.model.roadobject.LocalizedString
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectProvider
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class JunctionTest {

    @Test
    fun `type is JCT`() {
        val actual = Junction(
            "id",
            listOf(LocalizedString("en", "name")),
            1.0,
            RoadObjectProvider.MAPBOX,
            true,
            mockk(),
        )

        assertEquals(RoadObjectType.JCT, actual.objectType)
    }
}

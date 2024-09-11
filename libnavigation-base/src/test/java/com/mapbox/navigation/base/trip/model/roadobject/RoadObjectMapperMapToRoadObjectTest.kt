package com.mapbox.navigation.base.trip.model.roadobject

import com.mapbox.navigation.base.trip.model.roadobject.ic.Interchange
import com.mapbox.navigation.base.trip.model.roadobject.jct.Junction
import com.mapbox.navigator.IcInfo
import com.mapbox.navigator.JctInfo
import com.mapbox.navigator.MatchedRoadObjectLocation
import com.mapbox.navigator.RoadObjectMetadata
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadObjectMapperMapToRoadObjectTest {

    private val id = "some id"
    private val length = 1.2
    private val location = mockk<MatchedRoadObjectLocation>(relaxed = true)
    private val urban = true

    @Test
    fun ic_mapboxProvider_filled() {
        val language1 = "en"
        val name1 = "some name"
        val language2 = "it"
        val name2 = "qualche nome"
        val nativeObject = com.mapbox.navigator.RoadObject(
            id,
            length,
            location,
            com.mapbox.navigator.RoadObjectType.IC,
            com.mapbox.navigator.RoadObjectProvider.MAPBOX,
            RoadObjectMetadata.valueOf(
                IcInfo(
                    "id#0",
                    listOf(
                        com.mapbox.navigator.LocalizedString(language1, name1),
                        com.mapbox.navigator.LocalizedString(language2, name2),
                    ),
                ),
            ),
            urban,
        )
        val expected = Interchange(
            id,
            listOf(
                LocalizedString(language1, name1),
                LocalizedString(language2, name2),
            ),
            length,
            RoadObjectProvider.MAPBOX,
            urban,
            nativeObject,
        )

        val actual = nativeObject.mapToRoadObject()

        assertEquals(expected, actual)
    }

    @Test
    fun ic_customProvider_default() {
        val nativeObject = com.mapbox.navigator.RoadObject(
            id,
            null,
            location,
            com.mapbox.navigator.RoadObjectType.IC,
            com.mapbox.navigator.RoadObjectProvider.CUSTOM,
            RoadObjectMetadata.valueOf(IcInfo("id#1", emptyList())),
            null,
        )
        val expected = Interchange(
            id,
            emptyList(),
            null,
            RoadObjectProvider.CUSTOM,
            null,
            nativeObject,
        )

        val actual = nativeObject.mapToRoadObject()

        assertEquals(expected, actual)
    }

    @Test
    fun jct_mapboxProvider_filled() {
        val language1 = "en"
        val name1 = "some name"
        val language2 = "it"
        val name2 = "qualche nome"
        val nativeObject = com.mapbox.navigator.RoadObject(
            id,
            length,
            location,
            com.mapbox.navigator.RoadObjectType.JCT,
            com.mapbox.navigator.RoadObjectProvider.MAPBOX,
            RoadObjectMetadata.valueOf(
                JctInfo(
                    "id#2",
                    listOf(
                        com.mapbox.navigator.LocalizedString(language1, name1),
                        com.mapbox.navigator.LocalizedString(language2, name2),
                    ),
                ),
            ),
            urban,
        )
        val expected = Junction(
            id,
            listOf(
                LocalizedString(language1, name1),
                LocalizedString(language2, name2),
            ),
            length,
            RoadObjectProvider.MAPBOX,
            urban,
            nativeObject,
        )

        val actual = nativeObject.mapToRoadObject()

        assertEquals(expected, actual)
    }

    @Test
    fun jct_customProvider_default() {
        val nativeObject = com.mapbox.navigator.RoadObject(
            id,
            null,
            location,
            com.mapbox.navigator.RoadObjectType.JCT,
            com.mapbox.navigator.RoadObjectProvider.CUSTOM,
            RoadObjectMetadata.valueOf(JctInfo("id#3", emptyList())),
            null,
        )
        val expected = Junction(
            id,
            emptyList(),
            null,
            RoadObjectProvider.CUSTOM,
            null,
            nativeObject,
        )

        val actual = nativeObject.mapToRoadObject()

        assertEquals(expected, actual)
    }
}

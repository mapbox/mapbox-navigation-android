package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.navigator.RoadObjectType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private typealias SDKRoadObjectType =
    com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType

@RunWith(Parameterized::class)
internal class EHorizonMapperMapToRoadObjectTypeTest(
    private val input: RoadObjectType,
    private val expected: Int,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} to {1}")
        fun data(): Collection<Array<Any>> {
            val result = listOf(
                arrayOf<Any>(RoadObjectType.INCIDENT, SDKRoadObjectType.INCIDENT),
                arrayOf<Any>(
                    RoadObjectType.TOLL_COLLECTION_POINT,
                    SDKRoadObjectType.TOLL_COLLECTION
                ),
                arrayOf<Any>(
                    RoadObjectType.BORDER_CROSSING,
                    SDKRoadObjectType.COUNTRY_BORDER_CROSSING
                ),
                arrayOf<Any>(RoadObjectType.TUNNEL, SDKRoadObjectType.TUNNEL),
                arrayOf<Any>(RoadObjectType.RESTRICTED_AREA, SDKRoadObjectType.RESTRICTED_AREA),
                arrayOf<Any>(RoadObjectType.SERVICE_AREA, SDKRoadObjectType.REST_STOP),
                arrayOf<Any>(RoadObjectType.BRIDGE, SDKRoadObjectType.BRIDGE),
                arrayOf<Any>(RoadObjectType.CUSTOM, SDKRoadObjectType.CUSTOM),
                arrayOf<Any>(RoadObjectType.RAILWAY_CROSSING, SDKRoadObjectType.RAILWAY_CROSSING),
                arrayOf<Any>(RoadObjectType.IC, SDKRoadObjectType.IC),
                arrayOf<Any>(RoadObjectType.JCT, SDKRoadObjectType.JCT),
                arrayOf<Any>(RoadObjectType.NOTIFICATION, SDKRoadObjectType.NOTIFICATION),
            )
            assertEquals(RoadObjectType.values().size, result.size)
            return result
        }
    }

    @Test
    fun test() {
        assertEquals(expected, input.mapToRoadObjectType())
    }
}

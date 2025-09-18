package com.mapbox.navigation.core.adas

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasEdgeAttributesTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AdasEdgeAttributes::class.java)
            .verify()

        ToStringVerifier.forClass(AdasEdgeAttributes::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.EdgeAdasAttributes(
            listOf(AdasTypeFactory.NATIVE_SPEED_LIMIT_INFO),
            listOf(AdasTypeFactory.NATIVE_VALUE_ON_EDGE_1),
            listOf(AdasTypeFactory.NATIVE_VALUE_ON_EDGE_2),
            listOf(AdasTypeFactory.NATIVE_VALUE_ON_EDGE_3),
            true,
            true,
            com.mapbox.navigator.FormOfWay.CAR_PARK_ENTRANCE,
            com.mapbox.navigator.ETC2RoadType.HIGHWAY,
            listOf(AdasTypeFactory.NATIVE_ROAD_ITEM_ON_EDGE),
        )

        val platform = AdasEdgeAttributes.createFromNativeObject(native)

        assertEquals(listOf(AdasTypeFactory.SPEED_LIMIT_INFO), platform.speedLimit)
        assertEquals(listOf(AdasTypeFactory.VALUE_ON_EDGE_1), platform.slopes)
        assertEquals(listOf(AdasTypeFactory.VALUE_ON_EDGE_2), platform.elevations)
        assertEquals(listOf(AdasTypeFactory.VALUE_ON_EDGE_3), platform.curvatures)
        assertEquals(true, platform.isDividedRoad)
        assertEquals(true, platform.isBuiltUpArea)
        assertEquals(AdasEdgeAttributes.FormOfWay.CAR_PARK_ENTRANCE, platform.formOfWay)
        assertEquals(AdasEdgeAttributes.Etc2Road.HIGHWAY, platform.etc2)
        assertEquals(listOf(AdasTypeFactory.ROAD_ITEM_ON_EDGE), platform.roadItems)
    }
}

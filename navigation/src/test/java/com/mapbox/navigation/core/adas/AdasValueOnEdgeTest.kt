package com.mapbox.navigation.core.adas

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasValueOnEdgeTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AdasValueOnEdge::class.java)
            .verify()

        ToStringVerifier.forClass(AdasValueOnEdge::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.ValueOnEdge(0.1f, 0.2, 0.3)
        val platform = AdasValueOnEdge.createFromNativeObject(native)
        assertEquals(native.shapeIndex, platform.shapeIndex)
        assertEquals(native.percentAlong, platform.percentAlong, 0.0001)
        assertEquals(native.value, platform.value, 0.0001)
    }
}

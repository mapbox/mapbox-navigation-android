package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ConstellationTypeTest {

    @Test
    fun testNativeTypesMapping() {
        assertEquals(NativeConstellationType.UNKNOWN, ConstellationType.Unknown.nativeType)
        assertEquals(NativeConstellationType.GPS, ConstellationType.Gps.nativeType)
        assertEquals(NativeConstellationType.SBAS, ConstellationType.Sbas.nativeType)
        assertEquals(NativeConstellationType.GLONASS, ConstellationType.Glonass.nativeType)
        assertEquals(NativeConstellationType.QZSS, ConstellationType.Qzss.nativeType)
        assertEquals(NativeConstellationType.BEIDOU, ConstellationType.Beidou.nativeType)
        assertEquals(NativeConstellationType.GALILEO, ConstellationType.Galileo.nativeType)
        assertEquals(NativeConstellationType.IRNSS, ConstellationType.Irnss.nativeType)
    }
}

package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.physics.Speed.Companion.m_s

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object DataInputsTestDataFactory {

    val testRawGnssLocation = RawGnssLocation(
        altitude = 1.123F,
        bearing = 2.345F.degrees,
        speed = 3.789.m_s,
        latitude = 4.123F,
        longitude = -5.456F,
        bearingAccuracy = 6.789F.degrees,
        speedAccuracy = 7.123.m_s,
        horizontalAccuracyMeters = 8.456F,
        verticalAccuracyMeters = 9.789F,
    )

    val testRawGnssSatelliteData = RawGnssSatelliteData(
        svid = 1,
        carrierFrequencyHz = 2.123F,
        basebandCn0DbHz = 3.456,
        cn0DbHz = 4.789,
        usedInFix = true,
        hasEphemerisData = true,
        hasAlmanacData = true,
        constellationType = ConstellationType.Gps,
        azimuth = 5.789F.degrees,
        elevation = 6.123F.degrees,
    )
}

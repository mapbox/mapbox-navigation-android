package com.mapbox.navigation.core.datainputs

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

internal typealias NativeConstellationType = com.mapbox.navigator.ConstellationType

/**
 * Represents the different constellation types (the types of satellite navigation systems).
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class ConstellationType private constructor(
    internal val nativeType: NativeConstellationType,
) {

    /**
     * Unknown constellation type.
     */
    object Unknown : ConstellationType(NativeConstellationType.UNKNOWN)

    /**
     * GPS constellation type.
     */
    object Gps : ConstellationType(NativeConstellationType.GPS)

    /**
     * SBAS constellation type.
     */
    object Sbas : ConstellationType(NativeConstellationType.SBAS)

    /**
     * GLONASS constellation type.
     */
    object Glonass : ConstellationType(NativeConstellationType.GLONASS)

    /**
     * QZSS constellation type.
     */
    object Qzss : ConstellationType(NativeConstellationType.QZSS)

    /**
     * BEIDOU constellation type.
     */
    object Beidou : ConstellationType(NativeConstellationType.BEIDOU)

    /**
     * GALILEO constellation type.
     */
    object Galileo : ConstellationType(NativeConstellationType.GALILEO)

    /**
     * IRNSS constellation type.
     */
    object Irnss : ConstellationType(NativeConstellationType.IRNSS)
}

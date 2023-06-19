package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.ETCGateInfo

/**
 * Use the instance of this class to update the information about ETC gates.
 * Instance can be obtained via [MapboxNavigation.etcGateAPI].
 */
@ExperimentalPreviewMapboxNavigationAPI
class EtcGateApi internal constructor(
    internal var experimental: com.mapbox.navigator.Experimental
) {

    /**
     * Should be called as soon as vehicle passed Electronic Toll Collection(ETC) gate
     * which operates in Japan. This information can be used to improve map matching.
     * @param info - information about ETC gate which was just passed by vehicle.
     *
     * See Also: https://www.hanshin-exp.co.jp/english/drive/first-time/etc.html
     */
    fun updateEtcGateInfo(info: EtcGateInfo) {
        experimental.updateETCGateInfo(info.toNativeInfo())
    }

    private fun EtcGateInfo.toNativeInfo(): ETCGateInfo {
        return ETCGateInfo(id, monotonicTimestampNanoseconds)
    }
}

/**
 * Class containing information about ETC gate.
 * @param id gate id
 * @param monotonicTimestampNanoseconds timestamp which should be in sync with timestamps of
 *  locations from location provider
 */
@ExperimentalPreviewMapboxNavigationAPI
class EtcGateInfo(
    val id: Int,
    val monotonicTimestampNanoseconds: Long
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EtcGateInfo

        if (id != other.id) return false
        if (monotonicTimestampNanoseconds != other.monotonicTimestampNanoseconds) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + monotonicTimestampNanoseconds.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ETCGateInfo(id=$id, monotonicTimestampNanoseconds=$monotonicTimestampNanoseconds)"
    }
}

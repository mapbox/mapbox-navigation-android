package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.datainputs.DataInputsManager
import com.mapbox.navigation.core.datainputs.EtcGateInfo

/**
 * Use the instance of this class to update the information about ETC gates.
 * Instance can be obtained via [MapboxNavigation.etcGateAPI].
 *
 * Deprecated, use [DataInputsManager.updateEtcGateInfo] instead.
 */
@Deprecated("EtcGateApi is deprecated", ReplaceWith("DataInputsManager.updateEtcGateInfo"))
@ExperimentalPreviewMapboxNavigationAPI
class EtcGateApi internal constructor(
    internal var experimental: com.mapbox.navigator.Experimental,
) {

    /**
     * Should be called as soon as vehicle passed Electronic Toll Collection(ETC) gate
     * which operates in Japan. This information can be used to improve map matching.
     * @param info - information about ETC gate which was just passed by vehicle.
     *
     * See Also: https://www.hanshin-exp.co.jp/english/drive/first-time/etc.html
     */
    @Deprecated("EtcGateApi is deprecated", ReplaceWith("DataInputsManager.updateEtcGateInfo"))
    fun updateEtcGateInfo(info: EtcGateInfo) {
        experimental.updateETCGateInfo(info.mapToNative())
    }
}

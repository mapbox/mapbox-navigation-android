package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.datainputs.EtcGateInfo
import com.mapbox.navigator.ETCGateInfo
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EtcGateApiTest {

    private val experimental = mockk<com.mapbox.navigator.Experimental>(relaxed = true)

    private val api = EtcGateApi(experimental)

    @Test
    fun updateEtcGateInfo() {
        api.updateEtcGateInfo(EtcGateInfo(13, 8723468273648))
        verify(exactly = 1) {
            experimental.updateETCGateInfo(ETCGateInfo(13, 8723468273648))
        }
    }

    @Test
    fun updateEtcGateInfoAfterExperimentalChange() {
        val experimental2 = mockk<com.mapbox.navigator.Experimental>(relaxed = true)
        api.experimental = experimental2

        api.updateEtcGateInfo(EtcGateInfo(13, 8723468273648))

        verify(exactly = 0) { experimental.updateETCGateInfo(any()) }
        verify(exactly = 1) {
            experimental2.updateETCGateInfo(ETCGateInfo(13, 8723468273648))
        }
    }
}

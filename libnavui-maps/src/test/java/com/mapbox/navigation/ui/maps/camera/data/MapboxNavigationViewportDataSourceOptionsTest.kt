package com.mapbox.navigation.ui.maps.camera.data

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class MapboxNavigationViewportDataSourceOptionsTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(FollowingFrameOptions.FocalPoint::class.java).verify()
        ToStringVerifier.forClass(FollowingFrameOptions.FocalPoint::class.java).verify()
    }
}

package com.mapbox.navigation.ui.maps.camera.data

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.testing.withPrefabTestPoint
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class ViewportDataTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(ViewportData::class.java)
            .withPrefabTestPoint()
            .verify()

        ToStringVerifier.forClass(ViewportData::class.java)
            .withPrefabTestPoint()
            .verify()
    }
}

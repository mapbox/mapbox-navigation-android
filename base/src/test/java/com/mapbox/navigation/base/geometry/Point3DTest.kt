package com.mapbox.navigation.base.geometry

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.internal.mapToNative
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

class Point3DTest {

    @Test
    fun testMapToNative() {
        val testData = Point3D(
            x = 1.0,
            y = 2.0,
            z = 3.0,
        )

        val native = testData.mapToNative()

        assertEquals(1.0F, native.x)
        assertEquals(2.0F, native.y)
        assertEquals(3.0F, native.z)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(Point3D::class.java)
            .verify()

        ToStringVerifier.forClass(Point3D::class.java)
            .verify()
    }
}

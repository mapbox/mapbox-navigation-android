package com.mapbox.navigation.base.physics

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.internal.mapToNativePoint3DRadiansPerSecond
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AngularVelocity3DTest {

    @Test
    fun testConvertRadiansToDegrees() {
        val radiansPerSecond = AngularVelocity3D.radiansPerSecond(
            x = 1.0,
            y = 2.0,
            z = 3.0,

        )
        val degreesPerSecond = radiansPerSecond.convert(AngularVelocityUnit.DEGREES_PER_SECOND)

        assertEquals(AngularVelocityUnit.DEGREES_PER_SECOND, degreesPerSecond.unit)
        assertEquals(Math.toDegrees(1.0), degreesPerSecond.x, 0.0001)
        assertEquals(Math.toDegrees(2.0), degreesPerSecond.y, 0.0001)
        assertEquals(Math.toDegrees(3.0), degreesPerSecond.z, 0.0001)
    }

    @Test
    fun testConvertDegreesToRadians() {
        val degreesPerSecond = AngularVelocity3D.degreesPerSecond(
            x = 90.0,
            y = 180.0,
            z = 270.0,
        )
        val radiansPerSecond = degreesPerSecond.convert(AngularVelocityUnit.RADIANS_PER_SECOND)

        assertEquals(AngularVelocityUnit.RADIANS_PER_SECOND, radiansPerSecond.unit)
        assertEquals(Math.toRadians(90.0), radiansPerSecond.x, 0.0001)
        assertEquals(Math.toRadians(180.0), radiansPerSecond.y, 0.0001)
        assertEquals(Math.toRadians(270.0), radiansPerSecond.z, 0.0001)
    }

    @Test
    fun testConvertToSameUnit() {
        val degreesPerSecond = AngularVelocity3D.degreesPerSecond(
            x = 90.0,
            y = 180.0,
            z = 270.0,
        )

        assertSame(
            degreesPerSecond,
            degreesPerSecond.convert(AngularVelocityUnit.DEGREES_PER_SECOND),
        )
    }

    @Test
    fun testConvertRadiansToNativePoint3DRadiansPerSecond() {
        val radiansPerSecond = AngularVelocity3D.radiansPerSecond(
            x = 1.0,
            y = 2.0,
            z = 3.0,
        )

        val native = radiansPerSecond.mapToNativePoint3DRadiansPerSecond()

        assertEquals(1F, native.x, 0.0001F)
        assertEquals(2F, native.y, 0.0001F)
        assertEquals(3F, native.z, 0.0001F)
    }

    @Test
    fun testConvertDegreesToNativePoint3DRadiansPerSecond() {
        val degreesPerSecond = AngularVelocity3D.degreesPerSecond(
            x = 90.0,
            y = 180.0,
            z = 270.0,
        )

        val native = degreesPerSecond.mapToNativePoint3DRadiansPerSecond()

        assertEquals(Math.toRadians(90.0).toFloat(), native.x, 0.0001F)
        assertEquals(Math.toRadians(180.0).toFloat(), native.y, 0.0001F)
        assertEquals(Math.toRadians(270.0).toFloat(), native.z, 0.0001F)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AngularVelocity3D::class.java)
            .verify()

        ToStringVerifier.forClass(AngularVelocity3D::class.java)
            .verify()
    }
}

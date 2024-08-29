package com.mapbox.navigation.base.geometry

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.geometry.Angle.Companion.toAngle
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class AngleTest {

    @Test
    fun testConvertToDegrees() {
        val angleInRadians = (Math.PI / 2).radians
        val convertedAngle = angleInRadians.convert(AngleUnit.DEGREES)
        assertEquals(90.0, convertedAngle.value, 0.0001)
        assertEquals(AngleUnit.DEGREES, convertedAngle.unit)
    }

    @Test
    fun testConvertToRadians() {
        val angleInDegrees = 180.0.degrees
        val convertedAngle = angleInDegrees.convert(AngleUnit.RADIANS)
        assertEquals(Math.PI, convertedAngle.value, 0.0001)
        assertEquals(AngleUnit.RADIANS, convertedAngle.unit)
    }

    @Test
    fun testConvertToSameUnit() {
        val angleInDegrees = 270.0.degrees
        val convertedAngle = angleInDegrees.convert(AngleUnit.DEGREES)
        assertSame(angleInDegrees, convertedAngle)
    }

    @Test
    fun testToDoubleAsDegrees() {
        val angleInRadians = Math.PI.radians
        val convertedValue = angleInRadians.toDouble(AngleUnit.DEGREES)
        assertEquals(180.0, convertedValue, 0.0001)
    }

    @Test
    fun testToDoubleAsRadians() {
        val angleInDegrees = 360.0.degrees
        val convertedValue = angleInDegrees.toDouble(AngleUnit.RADIANS)
        assertEquals(2 * Math.PI, convertedValue, 0.0001)
    }

    @Test
    fun testToFloatAsDegrees() {
        val angleInRadians = Math.PI.radians
        val convertedValue = angleInRadians.toFloat(AngleUnit.DEGREES)
        assertEquals(180F, convertedValue)
    }

    @Test
    fun testToFloatAsRadians() {
        val angleInDegrees = 360.0.degrees
        val convertedValue = angleInDegrees.toFloat(AngleUnit.RADIANS)
        assertEquals((2.0 * Math.PI).toFloat(), convertedValue)
    }

    @Test
    fun testDegreesExtension() {
        val degreesAngle = 90.0.degrees
        assertEquals(90.0, degreesAngle.value)
        assertEquals(AngleUnit.DEGREES, degreesAngle.unit)
    }

    @Test
    fun testRadiansExtension() {
        val radiansAngle = (Math.PI / 2).radians
        assertEquals(Math.PI / 2, radiansAngle.value, 0.0001)
        assertEquals(AngleUnit.RADIANS, radiansAngle.unit)
    }

    @Test
    fun testNumberExtensions() {
        val degreesAngle = 360.0.toAngle(AngleUnit.DEGREES)
        assertEquals(360.0, degreesAngle.value)
        assertEquals(AngleUnit.DEGREES, degreesAngle.unit)

        val radiansAngle = (Math.PI / 2).toAngle(AngleUnit.RADIANS)
        assertEquals(Math.PI / 2, radiansAngle.value, 0.0001)
        assertEquals(AngleUnit.RADIANS, radiansAngle.unit)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(Angle::class.java)
            .verify()

        ToStringVerifier.forClass(Angle::class.java)
            .verify()
    }
}

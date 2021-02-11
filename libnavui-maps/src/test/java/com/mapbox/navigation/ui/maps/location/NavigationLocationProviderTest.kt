package com.mapbox.navigation.ui.maps.location

import android.animation.ValueAnimator
import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigationLocationProviderTest {

    private lateinit var navigationLocationProvider: NavigationLocationProvider

    @Before
    fun setup() {
        navigationLocationProvider = NavigationLocationProvider()
    }

    @Test
    fun `location is cached`() {
        val location: Location = mockk()

        navigationLocationProvider.changePosition(location)

        assertEquals(location, navigationLocationProvider.lastLocation)
    }

    @Test
    fun `keyPoints are cached`() {
        val location: Location = mockk()
        val keyPoints: List<Location> = mockk()

        navigationLocationProvider.changePosition(location, keyPoints)

        assertEquals(keyPoints, navigationLocationProvider.lastKeyPoints)
    }

    @Test
    fun `location consumers are notified on update, without key points`() {
        val location: Location = mockk {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { bearing } returns 123f
        }
        val keyPoints: List<Location> = emptyList()
        val latLngOptions: (ValueAnimator.() -> Unit) = mockk()
        val bearingOptions: (ValueAnimator.() -> Unit) = mockk()
        val consumer: LocationConsumer = mockk(relaxUnitFun = true)

        navigationLocationProvider.registerLocationConsumer(consumer)
        navigationLocationProvider.changePosition(
            location,
            keyPoints,
            latLngOptions,
            bearingOptions
        )

        val expectedPoints =
            arrayOf(Point.fromLngLat(location.longitude, location.latitude))
        verify(exactly = 1) { consumer.onLocationUpdated(*expectedPoints, options = latLngOptions) }
        val expectedBearings = doubleArrayOf(location.bearing.toDouble())
        verify(exactly = 1) {
            consumer.onBearingUpdated(*expectedBearings, options = bearingOptions)
        }
    }

    @Test
    fun `location consumers are notified on update, with key points`() {
        val location: Location = mockk {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { bearing } returns 123f
        }
        val keyPoint: Location = mockk {
            every { latitude } returns 30.0
            every { longitude } returns 40.0
            every { bearing } returns 456f
        }
        val keyPoints: List<Location> = listOf(keyPoint, location)
        val latLngOptions: (ValueAnimator.() -> Unit) = mockk()
        val bearingOptions: (ValueAnimator.() -> Unit) = mockk()
        val consumer: LocationConsumer = mockk(relaxUnitFun = true)

        navigationLocationProvider.registerLocationConsumer(consumer)
        navigationLocationProvider.changePosition(
            location,
            keyPoints,
            latLngOptions,
            bearingOptions
        )

        val expectedPoints = arrayOf(
            Point.fromLngLat(keyPoint.longitude, keyPoint.latitude),
            Point.fromLngLat(location.longitude, location.latitude),
        )
        verify(exactly = 1) { consumer.onLocationUpdated(*expectedPoints, options = latLngOptions) }
        val expectedBearings = doubleArrayOf(
            keyPoint.bearing.toDouble(),
            location.bearing.toDouble()
        )
        verify(exactly = 1) {
            consumer.onBearingUpdated(*expectedBearings, options = bearingOptions)
        }
    }

    @Test
    fun `location consumers are notified immediately on registration`() {
        val location: Location = mockk {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { bearing } returns 123f
        }
        val keyPoint: Location = mockk {
            every { latitude } returns 30.0
            every { longitude } returns 40.0
            every { bearing } returns 456f
        }
        val keyPoints: List<Location> = listOf(keyPoint, location)
        val consumer: LocationConsumer = mockk(relaxUnitFun = true)
        val latLngAnimator: ValueAnimator = mockk {
            every { setDuration(any()) } returns this
        }
        every { consumer.onLocationUpdated(*anyVararg(), options = captureLambda()) } answers {
            secondArg<(ValueAnimator.() -> Unit)>().invoke(latLngAnimator)
        }
        val bearingAnimator: ValueAnimator = mockk {
            every { setDuration(any()) } returns this
        }
        every { consumer.onBearingUpdated(*anyDoubleVararg(), options = captureLambda()) } answers {
            lambda<(ValueAnimator.() -> Unit)>().captured.invoke(bearingAnimator)
        }

        navigationLocationProvider.changePosition(
            location,
            keyPoints
        )
        navigationLocationProvider.registerLocationConsumer(consumer)

        val expectedPoints = arrayOf(
            Point.fromLngLat(keyPoint.longitude, keyPoint.latitude),
            Point.fromLngLat(location.longitude, location.latitude),
        )
        verify(exactly = 1) { consumer.onLocationUpdated(*expectedPoints, options = any()) }
        val expectedBearings = doubleArrayOf(
            keyPoint.bearing.toDouble(),
            location.bearing.toDouble()
        )
        verify(exactly = 1) {
            consumer.onBearingUpdated(*expectedBearings, options = any())
        }
        verify { latLngAnimator.duration = 0 }
        verify { bearingAnimator.duration = 0 }
    }

    @Test
    fun `location consumers aren't notified when unregistered`() {
        val location: Location = mockk {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { bearing } returns 123f
        }
        val keyPoints: List<Location> = emptyList()
        val latLngOptions: (ValueAnimator.() -> Unit) = mockk()
        val bearingOptions: (ValueAnimator.() -> Unit) = mockk()
        val consumer: LocationConsumer = mockk(relaxUnitFun = true)

        navigationLocationProvider.registerLocationConsumer(consumer)
        navigationLocationProvider.unRegisterLocationConsumer(consumer)
        navigationLocationProvider.changePosition(
            location,
            keyPoints,
            latLngOptions,
            bearingOptions
        )

        verify(exactly = 0) { consumer.onLocationUpdated(any()) }
        verify(exactly = 0) {
            consumer.onBearingUpdated(any())
        }
    }
}

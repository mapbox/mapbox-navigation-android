package com.mapbox.navigation.base.options

import com.mapbox.common.location.AccuracyLevel
import com.mapbox.common.location.DeviceLocationProviderFactory
import com.mapbox.common.location.IntervalSettings
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationOptionsTest {

    @Test
    fun defaultRequest() {
        val options = LocationOptions.Builder().build()

        assertEquals(AccuracyLevel.HIGH, options.request.accuracy)
        assertEquals(
            IntervalSettings.Builder()
                .minimumInterval(500)
                .interval(1000L)
                .build(),
            options.request.interval,
        )
        assertNull(options.request.displacement)
    }

    @Test
    fun defaultFactory() {
        val options = LocationOptions.Builder().build()

        assertNull(options.locationProviderFactory)
        assertEquals(LocationOptions.LocationProviderType.REAL, options.locationProviderType)
    }

    @Test
    fun customFactory() {
        val customFactory = mockk<DeviceLocationProviderFactory>()
        val options = LocationOptions.Builder()
            .locationProviderFactory(customFactory, LocationOptions.LocationProviderType.MIXED)
            .build()

        assertEquals(customFactory, options.locationProviderFactory)
        assertEquals(LocationOptions.LocationProviderType.MIXED, options.locationProviderType)
    }

    @Test
    fun invalidType() {
        val customFactory = mockk<DeviceLocationProviderFactory>()
        assertThrows(IllegalArgumentException::class.java) {
            LocationOptions.Builder()
                .locationProviderFactory(customFactory, "invalid")
                .build()
        }
    }

    // If this fails, it means that you've added a new possible value of LocationProviderType,
    // but didn't add it to acceptedValues list.
    // This will not make it possible to use this new value, because the builder will throw.
    // Action required: populate the list with the new item.
    @Test
    fun acceptedValues() {
        assertEquals(
            LocationOptions.LocationProviderType::class.java.declaredFields
                .filter { it.type == String::class.java }.size,
            LocationOptions.LocationProviderType.acceptedValues.size,
        )
    }

    @Test
    fun throwsWhenCustomFactorySetAfterDefaultSourceSet() {
        val customFactory = mockk<DeviceLocationProviderFactory>()
        assertThrows(IllegalArgumentException::class.java) {
            LocationOptions.Builder()
                .defaultLocationProviderSource(LocationOptions.LocationProviderSource.BEST)
                .locationProviderFactory(customFactory, LocationOptions.LocationProviderType.REAL)
                .build()
        }
    }

    @Test
    fun throwsWhenDefaultSourceSetAfterCustomFactorySet() {
        val customFactory = mockk<DeviceLocationProviderFactory>()
        assertThrows(IllegalArgumentException::class.java) {
            LocationOptions.Builder()
                .locationProviderFactory(customFactory, LocationOptions.LocationProviderType.REAL)
                .defaultLocationProviderSource(LocationOptions.LocationProviderSource.BEST)
                .build()
        }
    }

    @Test
    fun sourceDefaultsToBest() {
        val options = LocationOptions.Builder().build()
        assertEquals(LocationOptions.LocationProviderSource.BEST, options.locationProviderSource)
    }

    @Test
    fun sourceIsSet() {
        val sources = LocationOptions.LocationProviderSource.Companion::class.java.declaredFields
            .filter { it.type == LocationOptions.LocationProviderSource::class.java }
            .map { it.isAccessible = true; it.get(null) as LocationOptions.LocationProviderSource }

        sources.forEach { source ->
            val options = LocationOptions.Builder()
                .defaultLocationProviderSource(source)
                .build()
            assertEquals(source, options.locationProviderSource)
        }
    }
}

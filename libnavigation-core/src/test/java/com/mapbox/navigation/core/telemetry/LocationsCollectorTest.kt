package com.mapbox.navigation.core.telemetry

import android.location.Location
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@ExperimentalCoroutinesApi
class LocationsCollectorTest {

    private val locationsCollector = LocationsCollectorImpl(null)

    @Test
    fun ignoreEnhancedLocationUpdates() {
        locationsCollector.onEnhancedLocationChanged(mockk(), mockk())

        assertNull(locationsCollector.lastLocation)
    }

    @Test
    fun useRawLocationUpdates() {
        val rawLocation: Location = mockk()
        locationsCollector.onRawLocationChanged(rawLocation)

        assertEquals(rawLocation, locationsCollector.lastLocation)
    }

    @Test
    fun lastLocation() = runBlocking {
        val firstLocation = mockk<Location>()
        val secondLocation = mockk<Location>()

        locationsCollector.onRawLocationChanged(firstLocation)
        assertEquals(firstLocation, locationsCollector.lastLocation)

        locationsCollector.onRawLocationChanged(secondLocation)
        assertEquals(secondLocation, locationsCollector.lastLocation)
    }

    @Test
    fun preAndPostLocationsOrder() = runBlocking {
        val preEventLocation = mockk<Location>()
        val postEventLocation = mockk<Location>()

        locationsCollector.onRawLocationChanged(preEventLocation)
        locationsCollector.collectLocations { preEventLocations, postEventLocations ->
            assertEquals(1, preEventLocations.size)
            assertEquals(preEventLocation, preEventLocations[0])

            assertEquals(1, postEventLocations.size)
            assertEquals(postEventLocation, postEventLocations[0])
        }
        locationsCollector.onRawLocationChanged(postEventLocation)
        locationsCollector.flushBuffers()
    }

    @Test
    fun preAndPostLocationsMaxSize() = runBlocking {
        repeat(25) { locationsCollector.onRawLocationChanged(mockk()) }
        locationsCollector.collectLocations { preEventLocations, postEventLocations ->
            assertEquals(20, preEventLocations.size)
            assertEquals(20, postEventLocations.size)
        }
        repeat(25) { locationsCollector.onRawLocationChanged(mockk()) }
        locationsCollector.flushBuffers()
    }

    @Test
    fun prePostLocationsEvents() = runBlocking {
        val l = mutableListOf<Location>()
        repeat(42) { l.add(mockk()) }

        // before any location posted. preList will be empty. postList will have 20 items
        locationsCollector.collectLocations { preLocations, postLocations ->
            val preList = emptyList<Location>()
            val postList = mutableListOf<Location>().apply { for (i in 0 until 20) add(l[i]) }
            assertEquals(preList, preLocations)
            assertEquals(postList, postLocations)
        }

        for (i in 0 until 5) locationsCollector.onRawLocationChanged(l[i])

        // 5 locations posted. preList will have all of them. postList will have 20 items
        locationsCollector.collectLocations { preLocations, postLocations ->
            val preList = mutableListOf<Location>().apply { for (i in 0 until 5) add(l[i]) }
            val postList = mutableListOf<Location>().apply { for (i in 5 until 25) add(l[i]) }
            assertEquals(preList, preLocations)
            assertEquals(postList, postLocations)
        }

        for (i in 5 until 17) locationsCollector.onRawLocationChanged(l[i])

        // 17 locations posted. preList will have all of them. postList will have 20 items
        locationsCollector.collectLocations { preLocations, postLocations ->
            val preList = mutableListOf<Location>().apply { for (i in 0 until 17) add(l[i]) }
            val postList = mutableListOf<Location>().apply { for (i in 17 until 37) add(l[i]) }
            assertEquals(preList, preLocations)
            assertEquals(postList, postLocations)
        }

        for (i in 17 until 29) locationsCollector.onRawLocationChanged(l[i])

        // 29 locations posted. preList will have the last 20. postList will have 13 items
        locationsCollector.collectLocations { preLocations, postLocations ->
            val preList = mutableListOf<Location>().apply { for (i in 9 until 29) add(l[i]) }
            val postList = mutableListOf<Location>().apply { for (i in 29 until 42) add(l[i]) }
            assertEquals(preList, preLocations)
            assertEquals(postList, postLocations)
        }

        for (i in 29 until 42) locationsCollector.onRawLocationChanged(l[i])

        // 42 locations posted. preList will have the last 20. postList will be empty
        locationsCollector.collectLocations { preLocations, postLocations ->
            val preList = mutableListOf<Location>().apply { for (i in 22 until 42) add(l[i]) }
            val postList = emptyList<Location>()
            assertEquals(preList, preLocations)
            assertEquals(postList, postLocations)
        }

        locationsCollector.flushBuffers()
    }
}

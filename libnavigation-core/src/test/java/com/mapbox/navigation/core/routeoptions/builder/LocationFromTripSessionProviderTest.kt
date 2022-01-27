package com.mapbox.navigation.core.routeoptions.builder

import android.location.Location
import com.mapbox.navigation.core.infra.factories.createLocation
import com.mapbox.navigation.core.infra.factories.createLocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.TripSessionLocationProvider
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class LocationFromTripSessionProviderTest {

    @Test
    fun `current location is taken from the trip session`() = runBlocking<Unit> {
        val testTripSession = TestTripSession().apply {
            updateMatchedLocation(
                createLocationMatcherResult(
                    enhancedLocation = createLocation(
                        longitude = 1.0,
                        latitude = 2.0,
                        bearing = 3.0
                    ),
                    zLevel = 4
                )
            )
        }
        val locationProvider = createLocationFromTripSessionProvider(testTripSession)

        val currentLocation = locationProvider.getCurrentLocation()

        assertEquals(1.0, currentLocation.point.longitude())
        assertEquals(2.0, currentLocation.point.latitude())
        assertEquals(3.0, currentLocation.bearing)
        assertEquals(4, currentLocation.zLevel)
    }

    @Test
    fun `wait for update if location isn't available yet`() = runBlocking<Unit> {
        val testTripSession = TestTripSession()
        val locationProvider = createLocationFromTripSessionProvider(testTripSession)

        val currentLocationDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            locationProvider.getCurrentLocation()
        }
        assertFalse(currentLocationDeferred.isCompleted)

        testTripSession.updateMatchedLocation(
            createLocationMatcherResult(
                enhancedLocation = createLocation(
                    longitude = 1.0,
                    latitude = 2.0,
                    bearing = 3.0
                ),
                zLevel = 4
            )
        )
        val currentLocation = currentLocationDeferred.await()
        assertEquals(1.0, currentLocation.point.longitude())
        assertEquals(2.0, currentLocation.point.latitude())
        assertEquals(3.0, currentLocation.bearing)
        assertEquals(4, currentLocation.zLevel)
        testTripSession.verifyNoActiveSubscriptions()
    }

    @Test
    fun `cancel getting current location`() = runBlocking<Unit> {
        val testTripSession = TestTripSession()
        val locationProvider = createLocationFromTripSessionProvider(testTripSession)
        val getLocationTask = async(start = CoroutineStart.UNDISPATCHED) {
            locationProvider.getCurrentLocation()
        }
        testTripSession.verifySomeActiveSubscriptions()

        getLocationTask.cancel()

        testTripSession.verifyNoActiveSubscriptions()
    }

    @Test
    fun `timeout if can't get current location in reasonable time`() = runBlockingTest {
        val testTripSession = TestTripSession()
        val locationProvider = createLocationFromTripSessionProvider(testTripSession)

        val getLocationTask = async(start = CoroutineStart.UNDISPATCHED) {
            locationProvider.getCurrentLocation()
        }
        advanceTimeBy(31_000)

        assertTrue("get location is still in progress", getLocationTask.isCompleted)
        assertNotNull(getLocationTask.getCompletionExceptionOrNull())
        testTripSession.verifyNoActiveSubscriptions()
    }
}

private fun createLocationFromTripSessionProvider(
    tripSessionLocationProvider: TripSessionLocationProvider = TestTripSession()
) = LocationFromTripSessionProvider(
    tripSessionLocationProvider = tripSessionLocationProvider
)

class TestTripSession : TripSessionLocationProvider {

    private val locationObservers = mutableListOf<LocationObserver>()

    override fun getRawLocation(): Location = TODO("implement if you need it")
    override val zLevel: Int? get() = locationMatcherResult?.zLevel
    override var locationMatcherResult: LocationMatcherResult? = null

    override fun registerLocationObserver(locationObserver: LocationObserver) {
        locationObservers.add(locationObserver)
    }

    override fun unregisterLocationObserver(locationObserver: LocationObserver) {
        locationObservers.remove(locationObserver)
    }

    override fun unregisterAllLocationObservers() {
        locationObservers.clear()
    }

    fun updateMatchedLocation(
        locationMatcherResult: LocationMatcherResult,
    ) {
        this.locationMatcherResult = locationMatcherResult
        locationObservers.forEach { it.onNewLocationMatcherResult(locationMatcherResult) }
    }

    fun verifySomeActiveSubscriptions() {
        assertNotSame(0, locationObservers.size)
    }

    fun verifyNoActiveSubscriptions() {
        assertEquals(0, locationObservers.size)
    }
}

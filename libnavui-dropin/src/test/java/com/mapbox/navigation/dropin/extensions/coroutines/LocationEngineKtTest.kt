package com.mapbox.navigation.dropin.extensions.coroutines

import android.location.Location
import android.location.LocationManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationEngineKtTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    lateinit var mockLocationEngine: LocationEngine

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.dropin.extensions.coroutines.LocationEngineKt")
        MockKAnnotations.init(this)
    }

    @Test
    fun `should call main getLastLocation method`() = coroutineRule.runBlockingTest {
        val location = Location(LocationManager.PASSIVE_PROVIDER).apply {
            latitude = 12.0
            longitude = 14.0
        }
        val callback = slot<LocationEngineCallback<LocationEngineResult>>()
        every { mockLocationEngine.getLastLocation(capture(callback)) } answers {
            callback.captured.onSuccess(LocationEngineResult.create(location))
        }

        val result = mockLocationEngine.getLastLocation()

        assertNotNull(result)
        assertEquals(location, result.lastLocation)
    }
}

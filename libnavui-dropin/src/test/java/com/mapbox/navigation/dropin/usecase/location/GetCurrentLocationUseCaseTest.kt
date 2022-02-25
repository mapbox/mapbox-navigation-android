package com.mapbox.navigation.dropin.usecase.location

import android.location.Location
import android.location.LocationManager
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GetCurrentLocationUseCaseTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    lateinit var mockLocationEngine: LocationEngine

    lateinit var stubLocationStateFlow: MutableStateFlow<Location?>

    lateinit var sut: GetCurrentLocationUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        stubLocationStateFlow = MutableStateFlow(null)
        val locationBehavior = mockk<LocationBehavior> {
            every { locationStateFlow } returns stubLocationStateFlow
        }
        val navigation = mockk<MapboxNavigation> {
            every { navigationOptions } returns mockk {
                every { locationEngine } returns mockLocationEngine
            }
        }

        sut = GetCurrentLocationUseCase(navigation, locationBehavior, Dispatchers.Main)
    }

    @Test
    fun `should return location from LocationBehavior`() = coroutineRule.runBlockingTest {
        val location = location(1.0, 2.0)
        stubLocationStateFlow.value = location

        val result = sut.invoke(Unit)

        assertTrue(result.isSuccess)
        assertEquals(location, result.getOrNull())
    }

    @Test
    fun `should fallback on MapboxNavigation LocationEngine`() = coroutineRule.runBlockingTest {
        val location = location(2.0, 3.0)
        val callback = slot<LocationEngineCallback<LocationEngineResult>>()
        every { mockLocationEngine.getLastLocation(capture(callback)) } answers {
            callback.captured.onSuccess(LocationEngineResult.create(location))
        }

        val result = sut.invoke(Unit)

        assertTrue(result.isSuccess)
        assertEquals(location, result.getOrNull())
    }

    private fun location(lat: Double, lon: Double) =
        Location(LocationManager.PASSIVE_PROVIDER).apply {
            latitude = lat
            longitude = lon
        }
}

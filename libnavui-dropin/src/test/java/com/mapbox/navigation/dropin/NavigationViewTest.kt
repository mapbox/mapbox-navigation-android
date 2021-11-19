package com.mapbox.navigation.dropin

import android.content.Context
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapInitOptions
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding
import com.mapbox.navigation.dropin.viewmodel.MapboxNavigationViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationViewTest {

    lateinit var ctx: Context

    private val mockFragmentActivity: FragmentActivity by lazy {
        mockk(relaxed = true) {
            every { resources } returns ctx.resources
            every { applicationInfo } returns ctx.applicationInfo
        }
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        mockkStatic(MapboxLayoutDropInViewBinding::class)
        mockkStatic(LayoutInflater::class)
        val viewBinding = mockk<MapboxLayoutDropInViewBinding>(relaxed = true)
        val mockInflater = mockk<LayoutInflater>()
        every { MapboxLayoutDropInViewBinding.inflate(any(), any()) } returns viewBinding
        every { LayoutInflater.from(any()) } returns mockInflater
    }

    @After
    fun tearDown() {
        unmockkStatic(LayoutInflater::class)
        unmockkStatic(MapboxLayoutDropInViewBinding::class)
    }

    @Test
    fun lifeCycleObserversAreAdded() {
        val navigationView = createNavigationView()
        val observerSlots = mutableListOf<LifecycleObserver>()

        verify(exactly = 2) { mockFragmentActivity.lifecycle.addObserver(capture(observerSlots)) }
        assertEquals(navigationView.lifecycleObserver, observerSlots[0])
        assertTrue(observerSlots[1] is MapboxNavigationViewModel)
    }

    @Test
    fun updateNavigationViewOptions() {
        val navigationView = createNavigationView()
        val replacementOptions = NavigationViewOptions.Builder(ctx).build()

        navigationView.updateNavigationViewOptions(replacementOptions)

        assertEquals(replacementOptions, navigationView.navigationViewOptions)
    }

    @Test
    fun addRouteProgressObserver() {
        val observer = mockk<RouteProgressObserver>()
        val navigationView = createNavigationView()

        navigationView.addRouteProgressObserver(observer)

        assertEquals(1, navigationView.externalRouteProgressObservers.size)
    }

    @Test
    fun removeRouteProgressObserver() {
        val observer = mockk<RouteProgressObserver>()
        val navigationView = createNavigationView()
        navigationView.addRouteProgressObserver(observer)
        assertEquals(1, navigationView.externalRouteProgressObservers.size)

        navigationView.removeRouteProgressObserver(observer)

        assertEquals(0, navigationView.externalRouteProgressObservers.size)
    }

    @Test
    fun addLocationObserver() {
        val observer = mockk<LocationObserver>()
        val navigationView = createNavigationView()

        navigationView.addLocationObserver(observer)

        assertEquals(1, navigationView.externalLocationObservers.size)
    }

    @Test
    fun removeLocationObserver() {
        val observer = mockk<LocationObserver>()
        val navigationView = createNavigationView()
        navigationView.addLocationObserver(observer)
        assertEquals(1, navigationView.externalLocationObservers.size)

        navigationView.removeLocationObserver(observer)

        assertEquals(0, navigationView.externalLocationObservers.size)
    }

    @Test
    fun addRoutesObserver() {
        val observer = mockk<RoutesObserver>()
        val navigationView = createNavigationView()

        navigationView.addRoutesObserver(observer)

        assertEquals(1, navigationView.externalRoutesObservers.size)
    }

    @Test
    fun removeRoutesObserver() {
        val observer = mockk<RoutesObserver>()
        val navigationView = createNavigationView()
        navigationView.addRoutesObserver(observer)
        assertEquals(1, navigationView.externalRoutesObservers.size)

        navigationView.removeRoutesObserver(observer)

        assertEquals(0, navigationView.externalRoutesObservers.size)
    }

    @Test
    fun addArrivalObserver() {
        val observer = mockk<ArrivalObserver>()
        val navigationView = createNavigationView()

        navigationView.addArrivalObserver(observer)

        assertEquals(1, navigationView.externalArrivalObservers.size)
    }

    @Test
    fun removeArrivalObserver() {
        val observer = mockk<ArrivalObserver>()
        val navigationView = createNavigationView()
        navigationView.addArrivalObserver(observer)
        assertEquals(1, navigationView.externalArrivalObservers.size)

        navigationView.removeArrivalObserver(observer)

        assertEquals(0, navigationView.externalArrivalObservers.size)
    }

    @Test
    fun addBannerInstructionsObserver() {
        val observer = mockk<BannerInstructionsObserver>()
        val navigationView = createNavigationView()

        navigationView.addBannerInstructionsObserver(observer)

        assertEquals(1, navigationView.externalBannerInstructionObservers.size)
    }

    @Test
    fun removeBannerInstructionsObserver() {
        val observer = mockk<BannerInstructionsObserver>()
        val navigationView = createNavigationView()
        navigationView.addBannerInstructionsObserver(observer)
        assertEquals(1, navigationView.externalBannerInstructionObservers.size)

        navigationView.removeBannerInstructionsObserver(observer)

        assertEquals(0, navigationView.externalBannerInstructionObservers.size)
    }

    @Test
    fun addTripSessionStateObserver() {
        val observer = mockk<TripSessionStateObserver>()
        val navigationView = createNavigationView()

        navigationView.addTripSessionStateObserver(observer)

        assertEquals(1, navigationView.externalTripSessionStateObservers.size)
    }

    @Test
    fun removeTripSessionStateObserver() {
        val observer = mockk<TripSessionStateObserver>()
        val navigationView = createNavigationView()
        navigationView.addTripSessionStateObserver(observer)
        assertEquals(1, navigationView.externalTripSessionStateObservers.size)

        navigationView.removeTripSessionStateObserver(observer)

        assertEquals(0, navigationView.externalTripSessionStateObservers.size)
    }

    @Test
    fun addVoiceInstructionObserver() {
        val observer = mockk<VoiceInstructionsObserver>()
        val navigationView = createNavigationView()

        navigationView.addVoiceInstructionObserver(observer)

        assertEquals(1, navigationView.externalVoiceInstructionsObservers.size)
    }

    @Test
    fun removeVoiceInstructionObserver() {
        val observer = mockk<VoiceInstructionsObserver>()
        val navigationView = createNavigationView()
        navigationView.addVoiceInstructionObserver(observer)
        assertEquals(1, navigationView.externalVoiceInstructionsObservers.size)

        navigationView.removeVoiceInstructionObserver(observer)

        assertEquals(0, navigationView.externalVoiceInstructionsObservers.size)
    }

    private fun createNavigationView(): NavigationView {
        return NavigationView(
            mockFragmentActivity,
            null,
            "token",
            MapInitOptions(ctx),
            NavigationViewOptions.Builder(ctx).build()
        )
    }
}

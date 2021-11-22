package com.mapbox.navigation.dropin

import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxNavigationViewApiTest {

    @Test
    fun addRouteProgressObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<RouteProgressObserver>()

        api.addRouteProgressObserver(observer)

        verify { navigationView.addRouteProgressObserver(observer) }
    }

    @Test
    fun removeRouteProgressObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<RouteProgressObserver>()

        api.removeRouteProgressObserver(observer)

        verify { navigationView.removeRouteProgressObserver(observer) }
    }

    @Test
    fun addLocationObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<LocationObserver>()

        api.addLocationObserver(observer)

        verify { navigationView.addLocationObserver(observer) }
    }

    @Test
    fun removeLocationObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<LocationObserver>()

        api.removeLocationObserver(observer)

        verify { navigationView.removeLocationObserver(observer) }
    }

    @Test
    fun addRoutesObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<RoutesObserver>()

        api.addRoutesObserver(observer)

        verify { navigationView.addRoutesObserver(observer) }
    }

    @Test
    fun removeRoutesObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<RoutesObserver>()

        api.removeRoutesObserver(observer)

        verify { navigationView.removeRoutesObserver(observer) }
    }

    @Test
    fun addArrivalObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<ArrivalObserver>()

        api.addArrivalObserver(observer)

        verify { navigationView.addArrivalObserver(observer) }
    }

    @Test
    fun removeArrivalObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<ArrivalObserver>()

        api.removeArrivalObserver(observer)

        verify { navigationView.removeArrivalObserver(observer) }
    }

    @Test
    fun addBannerInstructionsObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<BannerInstructionsObserver>()

        api.addBannerInstructionsObserver(observer)

        verify { navigationView.addBannerInstructionsObserver(observer) }
    }

    @Test
    fun removeBannerInstructionsObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<BannerInstructionsObserver>()

        api.removeBannerInstructionsObserver(observer)

        verify { navigationView.removeBannerInstructionsObserver(observer) }
    }

    @Test
    fun addTripSessionStateObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<TripSessionStateObserver>()

        api.addTripSessionStateObserver(observer)

        verify { navigationView.addTripSessionStateObserver(observer) }
    }

    @Test
    fun removeVoiceInstructionsObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<TripSessionStateObserver>()

        api.removeTripSessionStateObserver(observer)

        verify { navigationView.removeTripSessionStateObserver(observer) }
    }

    @Test
    fun addVoiceInstructionsObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<VoiceInstructionsObserver>()

        api.addVoiceInstructionsObserver(observer)

        verify { navigationView.addVoiceInstructionObserver(observer) }
    }

    @Test
    fun removeTripSessionStateObserver() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)
        val observer = mockk<VoiceInstructionsObserver>()

        api.removeVoiceInstructionsObserver(observer)

        verify { navigationView.removeVoiceInstructionObserver(observer) }
    }

    @Test
    fun getMapView() {
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)

        api.getMapView()

        verify { navigationView.retrieveMapView() }
    }

    @Test
    fun configureNavigationView() {
        val viewProvider = mockk<ViewProvider>()
        val navigationView = mockk<NavigationView>(relaxed = true)
        val api = MapboxNavigationViewApiImpl(navigationView)

        api.configureNavigationView(viewProvider)

        verify { navigationView.configure(viewProvider) }
    }
}

package com.mapbox.navigation.core.routealternatives

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class AllAlternativesObserversHolderTest {

    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    private val routesProgress = mockk<RouteProgress>(relaxed = true)
    private val routeOrigin = RouterOrigin.Onboard
    private val legacyAlternativesObserver = mockk<RouteAlternativesObserver>(relaxed = true)
    private val alternativesObserver = mockk<NavigationRouteAlternativesObserver>(relaxed = true)
    private val offboardRoutesObserver = mockk<OffboardRoutesObserver>(relaxed = true)
    private val onlineRoutes = listOf(mockk<NavigationRoute>())
    private val error = RouteAlternativesError("some error")
    private val firstAndLastObserverListener = mockk<FirstAndLastObserverListener>(relaxed = true)
    private val sut = AllAlternativesObserversHolder()

    @Test
    fun onRouteAlternatives_noObservers() {
        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
    }

    @Test
    fun onRouteAlternatives_hasObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 1) {
            legacyAlternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
            alternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
        }
        verify(exactly = 0) { offboardRoutesObserver.onOffboardRoutesAvailable(any()) }
    }

    @Test
    fun onRouteAlternatives_removedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.unregister(legacyAlternativesObserver)
        sut.unregister(alternativesObserver)
        sut.unregister(offboardRoutesObserver)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
        }
    }

    @Test
    fun onRouteAlternatives_clearedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
        }
    }

    @Test
    fun onRouteAlternatives_readdedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 1) {
            legacyAlternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
            alternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
        }
        verify(exactly = 0) { offboardRoutesObserver.onOffboardRoutesAvailable(any()) }
    }

    @Test
    fun onRouteAlternativesError_noObservers() {
        sut.onRouteAlternativesError(error)
    }

    @Test
    fun onRouteAlternativesError_hasObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onRouteAlternativesError(error)

        verify(exactly = 1) {
            alternativesObserver.onRouteAlternativesError(error)
        }
        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
        }
        verify(exactly = 1) {
            logger.logE("Error: some error", "RouteAlternativesController")
        }
    }

    @Test
    fun onRouteAlternativesError_removedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.unregister(legacyAlternativesObserver)
        sut.unregister(alternativesObserver)
        sut.unregister(offboardRoutesObserver)

        sut.onRouteAlternativesError(error)

        verify(exactly = 0) {
            alternativesObserver.onRouteAlternativesError(any())
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
            logger.logE(any(), any())
        }
    }

    @Test
    fun onRouteAlternativesError_clearedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()

        sut.onRouteAlternativesError(error)

        verify(exactly = 0) {
            alternativesObserver.onRouteAlternativesError(any())
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
            logger.logE(any(), any())
        }
    }

    @Test
    fun onRouteAlternativesError_readdedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onRouteAlternativesError(error)

        verify(exactly = 1) {
            alternativesObserver.onRouteAlternativesError(error)
        }
        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
        }
        verify(exactly = 1) {
            logger.logE("Error: some error", "RouteAlternativesController")
        }
    }

    @Test
    fun onOffboardRoutesAvailable_noObservers() {
        sut.onOffboardRoutesAvailable(onlineRoutes)
    }

    @Test
    fun onOffboardRoutesAvailable_hasObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onOffboardRoutesAvailable(onlineRoutes)

        verify(exactly = 1) {
            offboardRoutesObserver.onOffboardRoutesAvailable(onlineRoutes)
        }
        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternativesError(any())
        }
    }

    @Test
    fun onOffboardRoutesAvailable_removedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.unregister(legacyAlternativesObserver)
        sut.unregister(alternativesObserver)
        sut.unregister(offboardRoutesObserver)

        sut.onOffboardRoutesAvailable(onlineRoutes)

        verify(exactly = 0) {
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternativesError(any())
        }
    }

    @Test
    fun onOffboardRoutesAvailable_clearedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()

        sut.onOffboardRoutesAvailable(onlineRoutes)

        verify(exactly = 0) {
            offboardRoutesObserver.onOffboardRoutesAvailable(any())
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternativesError(any())
        }
    }

    @Test
    fun onOffboardRoutesAvailable_readdedObservers() {
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)
        sut.clear()
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.onOffboardRoutesAvailable(onlineRoutes)

        verify(exactly = 1) {
            offboardRoutesObserver.onOffboardRoutesAvailable(onlineRoutes)
        }
        verify(exactly = 0) {
            legacyAlternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternatives(any(), any(), any())
            alternativesObserver.onRouteAlternativesError(any())
        }
    }

    @Test
    fun selfRemovingLegacyAlternativesObserver() {
        val observer = object : RouteAlternativesObserver {
            override fun onRouteAlternatives(
                routeProgress: RouteProgress,
                alternatives: List<DirectionsRoute>,
                routerOrigin: RouterOrigin
            ) {
                sut.unregister(this)
            }
        }
        sut.register(observer)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
    }

    @Test
    fun selfRemovingAlternativesObserver() {
        val observer = object : NavigationRouteAlternativesObserver {
            override fun onRouteAlternatives(
                routeProgress: RouteProgress,
                alternatives: List<NavigationRoute>,
                routerOrigin: RouterOrigin
            ) {
                sut.unregister(this)
            }

            override fun onRouteAlternativesError(error: RouteAlternativesError) {
                sut.unregister(this)
            }
        }
        sut.register(observer)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
    }

    @Test
    fun selfRemovingOffboardRoutesObserver() {
        val observer = object : OffboardRoutesObserver {
            override fun onOffboardRoutesAvailable(routes: List<NavigationRoute>) {
                sut.unregister(this)
            }
        }
        sut.register(observer)

        sut.onOffboardRoutesAvailable(emptyList())
    }

    @Test
    fun duplicateLegacyAlternativesObserver() {
        sut.register(legacyAlternativesObserver)
        sut.register(legacyAlternativesObserver)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 1) {
            legacyAlternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
        }
    }

    @Test
    fun duplicateAlternativesObserver() {
        sut.register(alternativesObserver)
        sut.register(alternativesObserver)

        sut.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)

        verify(exactly = 1) {
            alternativesObserver.onRouteAlternatives(routesProgress, emptyList(), routeOrigin)
        }
    }

    @Test
    fun duplicateOffboardRoutesObserver() {
        sut.register(offboardRoutesObserver)
        sut.register(offboardRoutesObserver)

        sut.onOffboardRoutesAvailable(onlineRoutes)

        verify(exactly = 1) {
            offboardRoutesObserver.onOffboardRoutesAvailable(onlineRoutes)
        }
    }

    @Test
    fun removeNonExistentLegacyAlternativesObserver() {
        sut.unregister(legacyAlternativesObserver)
    }

    @Test
    fun onFirstObserver_legacyAlternativesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.register(legacyAlternativesObserver)

        verify(exactly = 1) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_legacyAlternativesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<RouteAlternativesObserver>(relaxed = true))
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(legacyAlternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_legacyAlternativesObserver_hasSameAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(legacyAlternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_legacyAlternativesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(legacyAlternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_legacyAlternativesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(legacyAlternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_alternativesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.register(alternativesObserver)

        verify(exactly = 1) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_alternativesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(alternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_alternativesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<NavigationRouteAlternativesObserver>(relaxed = true))
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(alternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_alternativesObserver_hasSameAlternativesObserver() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(alternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_alternativesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(alternativesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_offboardRoutesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.register(offboardRoutesObserver)

        verify(exactly = 1) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_offboardRoutesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(offboardRoutesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_offboardRoutesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(offboardRoutesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_offboardRoutesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<OffboardRoutesObserver>(relaxed = true))
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(offboardRoutesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onFirstObserver_offboardRoutesObserver_hasSameOffboardRoutesObserver() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)
        clearMocks(firstAndLastObserverListener, answers = false)

        sut.register(offboardRoutesObserver)

        verify(exactly = 0) {
            firstAndLastObserverListener.onFirstObserver()
        }
    }

    @Test
    fun onLastObserver_legacyAlternativesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)

        sut.unregister(legacyAlternativesObserver)

        verify(exactly = 1) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_legacyAlternativesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<RouteAlternativesObserver>(relaxed = true))
        sut.register(legacyAlternativesObserver)

        sut.unregister(legacyAlternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_legacyAlternativesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)
        sut.register(legacyAlternativesObserver)

        sut.unregister(legacyAlternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_legacyAlternativesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)
        sut.register(legacyAlternativesObserver)

        sut.unregister(legacyAlternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_alternativesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)

        sut.unregister(alternativesObserver)

        verify(exactly = 1) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_alternativesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)
        sut.register(alternativesObserver)

        sut.unregister(alternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_alternativesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<NavigationRouteAlternativesObserver>(relaxed = true))
        sut.register(alternativesObserver)

        sut.unregister(alternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_alternativesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)
        sut.register(alternativesObserver)

        sut.unregister(alternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_offboardRoutesObserver_noObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(offboardRoutesObserver)

        sut.unregister(offboardRoutesObserver)

        verify(exactly = 1) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_offboardRoutesObserver_hasLegacyAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(legacyAlternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.unregister(offboardRoutesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_offboardRoutesObserver_hasAlternativesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(alternativesObserver)
        sut.register(offboardRoutesObserver)

        sut.unregister(offboardRoutesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_offboardRoutesObserver_hasOffboardRoutesObservers() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)
        sut.register(mockk<OffboardRoutesObserver>(relaxed = true))
        sut.register(offboardRoutesObserver)

        sut.unregister(offboardRoutesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onLastObserver() }
    }

    @Test
    fun onLastObserver_nonExistentLegacyAlternativesObserver() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.unregister(legacyAlternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onFirstObserver() }
    }

    @Test
    fun onLastObserver_nonExistentAlternativesObserver() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.unregister(alternativesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onFirstObserver() }
    }

    @Test
    fun onLastObserver_nonExistentOffboardRoutesObserver() {
        sut.addFirstAndLastObserverListener(firstAndLastObserverListener)

        sut.unregister(offboardRoutesObserver)

        verify(exactly = 0) { firstAndLastObserverListener.onFirstObserver() }
    }
}

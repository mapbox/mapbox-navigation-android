package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalPreviewMapboxNavigationAPI
@Config(shadows = [ShadowReachabilityFactory::class, ShadowEventsService::class])
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(ParameterizedRobolectricTestRunner::class)
internal class MapboxNavigationRerouteInterruptionTest(
    private val previousState: RerouteState?,
    private val expectedNumberOfInterruptions: Int,
) : MapboxNavigationBaseTest() {

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): List<Array<Any?>> {
            return listOf(
                arrayOf(null, 0),
                arrayOf(RerouteState.Idle, 0),
                arrayOf(RerouteState.Interrupted, 0),
                arrayOf(RerouteState.FetchingRoute, 1),
                arrayOf(mockk<RerouteState.Failed>(), 0),
                arrayOf(mockk<RerouteState.RouteFetched>(), 0),
            )
        }
    }

    @Before
    override fun setUp() {
        super.setUp()
        createMapboxNavigation()
    }

    @Test
    fun setRerouteController() {
        val oldController = if (previousState != null) {
            mockk<RerouteController>(relaxed = true) {
                every { state } returns previousState
            }
        } else {
            null
        }
        val newController = mockk<RerouteController>(relaxed = true)
        oldController?.let { mapboxNavigation.setRerouteController(it) }

        mapboxNavigation.setRerouteController(newController)

        if (oldController != null) {
            verify(exactly = expectedNumberOfInterruptions) { oldController.interrupt() }
        }
        verify(exactly = expectedNumberOfInterruptions) { newController.reroute(any()) }
    }

    @Test
    fun setNullRerouteController() {
        val oldController = if (previousState != null) {
            mockk<RerouteController>(relaxed = true) {
                every { state } returns previousState
            }
        } else {
            null
        }
        val newController: RerouteController? = null
        oldController?.let { mapboxNavigation.setRerouteController(it) }

        mapboxNavigation.setRerouteController(newController)

        if (oldController != null) {
            verify(exactly = expectedNumberOfInterruptions) { oldController.interrupt() }
        }
    }

    @Test
    fun setNavigationRerouteController() {
        val oldController = if (previousState != null) {
            mockk<NavigationRerouteController>(relaxed = true) {
                every { state } returns previousState
            }
        } else {
            null
        }
        val newController = mockk<NavigationRerouteController>(relaxed = true)
        oldController?.let {
            mapboxNavigation.setRerouteController(it)
        }

        mapboxNavigation.setRerouteController(newController)

        if (oldController != null) {
            verify(exactly = expectedNumberOfInterruptions) { oldController.interrupt() }
        }
        verify(exactly = expectedNumberOfInterruptions) {
            newController.reroute(any<NavigationRerouteController.RoutesCallback>())
        }
    }

    @Test
    fun setNullNavigationRerouteController() {
        val oldController = if (previousState != null) {
            mockk<NavigationRerouteController>(relaxed = true) {
                every { state } returns previousState
            }
        } else {
            null
        }
        val newController: NavigationRerouteController? = null
        oldController?.let {
            mapboxNavigation.setRerouteController(it)
        }

        mapboxNavigation.setRerouteController(newController)

        if (oldController != null) {
            verify(exactly = expectedNumberOfInterruptions) { oldController.interrupt() }
        }
    }
}

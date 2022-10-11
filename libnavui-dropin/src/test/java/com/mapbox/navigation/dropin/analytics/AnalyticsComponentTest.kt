package com.mapbox.navigation.dropin.analytics

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.sendCustomEvent
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class AnalyticsComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val sut = AnalyticsComponent()
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk(relaxed = true)
        }
    }

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
    }

    @Test
    fun `when attached custom event is sent`() =
        coroutineRule.runBlockingTest {
            every { mockMapboxNavigation.sendCustomEvent(any(), any(), any()) } just Runs

            sut.onAttached(mapboxNavigation = mockMapboxNavigation)

            verify(exactly = 1) { mockMapboxNavigation.sendCustomEvent(any(), any(), any()) }
        }

    @Test
    fun `when detached followed by attached custom event is sent multiple times`() =
        coroutineRule.runBlockingTest {
            every { mockMapboxNavigation.sendCustomEvent(any(), any(), any()) } just Runs

            sut.onAttached(mapboxNavigation = mockMapboxNavigation)
            sut.onDetached(mapboxNavigation = mockMapboxNavigation)

            verify(exactly = 2) { mockMapboxNavigation.sendCustomEvent(any(), any(), any()) }
        }
}

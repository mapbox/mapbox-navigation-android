package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalPreviewMapboxNavigationAPI::class,
    ExperimentalCoroutinesApi::class,
    InternalCoroutinesApi::class
)
class ScalebarPlaceholderCoordinatorTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context = mockk<NavigationViewContext>()
    private val viewGroup = mockk<ViewGroup>()
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val coordinator = ScalebarPlaceholderCoordinator(context, viewGroup)

    @Test
    fun flowViewBindersReturnsOnlyScalebarPlaceholderBinder() = runBlockingTest {
        coordinator.apply {
            val binders = mapboxNavigation.flowViewBinders().toList()
            assertEquals(1, binders.size)
            assertTrue(binders[0] is ScalebarPlaceholderBinder)
        }
    }
}

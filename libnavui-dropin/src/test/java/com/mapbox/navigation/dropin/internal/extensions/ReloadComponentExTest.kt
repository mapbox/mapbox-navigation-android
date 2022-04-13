package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
class ReloadComponentExTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `reloadOnChange should re-create and re-attach childComponent on value change`() =
        coroutineRule.runBlockingTest {
            val child1 = mockk<UIComponent>(relaxed = true)
            val child2 = mockk<UIComponent>(relaxed = true)
            val flow = MutableStateFlow(0)
            val mapboxNavigation = mockk<MapboxNavigation>()
            val component = reloadOnChange(flow) {
                if (it % 2 == 0) child1 else child2
            }

            component.onAttached(mapboxNavigation)

            verify { child1.onAttached(mapboxNavigation) }
            flow.tryEmit(1)
            verify { child1.onDetached(mapboxNavigation) }
            verify { child2.onAttached(mapboxNavigation) }
        }
}

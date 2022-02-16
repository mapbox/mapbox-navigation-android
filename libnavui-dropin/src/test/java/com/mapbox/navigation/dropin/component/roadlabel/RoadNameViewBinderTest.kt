package com.mapbox.navigation.dropin.component.roadlabel

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
class RoadNameViewBinderTest {

    @Test
    fun `bind returns instance of RoadNameComponent`() {
        val ctx = mockk<Context>()
        val mockResources = mockk<Resources> {
            every { getResourceName(any()) } returns "roadNameView"
        }
        val mockRoadNameView = mockk<MapboxRoadNameView>()
        val mockView = mockk<View> {
            every { findViewById<View>(any()) } returns mockRoadNameView
        }
        val viewGroup = mockk<ViewGroup> {
            every { childCount } returns 1
            every { resources } returns mockResources
            every { getChildAt(0) } returns mockView
            every { context } returns ctx
        }

        val result = RoadNameViewBinder().bind(viewGroup)

        assertTrue(result is RoadNameComponent)
    }
}

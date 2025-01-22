package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigator.GraphAccessorInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class GraphAccessorTest {

    private lateinit var nativeGraphAccessor: GraphAccessorInterface
    private lateinit var navigator: MapboxNativeNavigator
    private lateinit var graphAccessor: GraphAccessor

    @Before
    fun setUp() {
        nativeGraphAccessor = mockk(relaxed = true)
        navigator = mockk(relaxed = true)
        every { navigator.graphAccessor } returns nativeGraphAccessor

        graphAccessor = GraphAccessor(navigator)
    }

    @Test
    fun `forwards getAdasisEdgeAttributes call to native`() {
        val edgeId = 123L

        graphAccessor.getAdasisEdgeAttributes(edgeId)

        verify(exactly = 1) {
            nativeGraphAccessor.getAdasAttributes(eq(edgeId))
        }
    }
}

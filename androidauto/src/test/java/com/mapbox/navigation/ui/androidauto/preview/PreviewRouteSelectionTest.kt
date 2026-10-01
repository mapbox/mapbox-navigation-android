package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.preview.RoutesPreview
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PreviewRouteSelectionTest {

    private val a = route("a")
    private val b = route("b")
    private val c = route("c")
    private val sut = PreviewRouteSelection()

    @Test
    fun `navigates the primary route when nothing is selected`() {
        assertEquals("a", sut.routeIdToNavigate(preview(primary = a, a, b)))
    }

    @Test
    fun `navigates the selected route before the preview reflects it`() {
        val preview = preview(primary = a, a, b)

        sut.onRouteSelected("b")

        assertEquals("b", sut.routeIdToNavigate(preview))
    }

    @Test
    fun `navigates the primary route once the selection is applied`() {
        val before = preview(primary = a, a, b)
        sut.onRouteSelected("b")
        val after = preview(primary = b, a, b)

        sut.onPreviewChanged(before, after)

        assertEquals("b", sut.routeIdToNavigate(after))
    }

    @Test
    fun `primary route changed elsewhere replaces the pending selection`() {
        // The car picked b, then the phone app made c the primary route.
        val before = preview(primary = a, a, b, c)
        sut.onRouteSelected("b")
        val after = preview(primary = c, a, b, c)

        sut.onPreviewChanged(before, after)

        assertEquals("c", sut.routeIdToNavigate(after))
    }

    @Test
    fun `new set of routes drops the pending selection`() {
        val x = route("x")
        val before = preview(primary = a, a, b)
        sut.onRouteSelected("b")
        val after = preview(primary = x, x)

        sut.onPreviewChanged(before, after)

        assertEquals("x", sut.routeIdToNavigate(after))
    }

    @Test
    fun `unchanged preview keeps the pending selection`() {
        val before = preview(primary = a, a, b)
        sut.onRouteSelected("b")

        sut.onPreviewChanged(before, preview(primary = a, a, b))

        assertEquals("b", sut.routeIdToNavigate(before))
    }

    private fun route(routeId: String) = mockk<NavigationRoute> {
        every { id } returns routeId
    }

    private fun preview(primary: NavigationRoute, vararg routes: NavigationRoute) =
        mockk<RoutesPreview> {
            every { originalRoutesList } returns routes.toList()
            every { routesList } returns listOf(primary) + routes.filter { it != primary }
        }
}

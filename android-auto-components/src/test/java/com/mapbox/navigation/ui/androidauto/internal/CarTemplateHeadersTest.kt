package com.mapbox.navigation.ui.androidauto.internal

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Suppress("DEPRECATION")
class CarTemplateHeadersTest : MapboxRobolectricTestRunner() {

    private var apiLevel = 0
    private val carContext: CarContext = mockk {
        every { carAppApiLevel } answers { apiLevel }
    }

    // The list, grid and message templates fill their legacy fields from setHeader, and build a
    // header from the legacy fields, so the branch is only visible in the builder calls.

    @Test
    fun `list template uses the header from Car API 7`() {
        apiLevel = 7
        val builder = mockk<ListTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", Action.BACK)

        val header = slot<Header>()
        verify { builder.setHeader(capture(header)) }
        verify(exactly = 0) { builder.setTitle(any<CharSequence>()) }
        verify(exactly = 0) { builder.setHeaderAction(any()) }
        assertEquals("Title", header.captured.title.toString())
        assertEquals(Action.BACK, header.captured.startHeaderAction)
    }

    @Test
    fun `list template uses the legacy setters below Car API 7`() {
        apiLevel = 6
        val builder = mockk<ListTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", Action.BACK)

        verify(exactly = 0) { builder.setHeader(any()) }
        verify { builder.setTitle("Title") }
        verify { builder.setHeaderAction(Action.BACK) }
    }

    @Test
    fun `grid template uses the header from Car API 7`() {
        apiLevel = 7
        val builder = mockk<GridTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", Action.BACK)

        val header = slot<Header>()
        verify { builder.setHeader(capture(header)) }
        verify(exactly = 0) { builder.setTitle(any<CharSequence>()) }
        verify(exactly = 0) { builder.setHeaderAction(any()) }
        assertEquals("Title", header.captured.title.toString())
        assertEquals(Action.BACK, header.captured.startHeaderAction)
    }

    @Test
    fun `grid template uses the legacy setters below Car API 7`() {
        apiLevel = 6
        val builder = mockk<GridTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", Action.BACK)

        verify(exactly = 0) { builder.setHeader(any()) }
        verify { builder.setTitle("Title") }
        verify { builder.setHeaderAction(Action.BACK) }
    }

    @Test
    fun `message template uses the header from Car API 7`() {
        apiLevel = 7
        val builder = mockk<MessageTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", startAction = null)

        val header = slot<Header>()
        verify { builder.setHeader(capture(header)) }
        verify(exactly = 0) { builder.setTitle(any<CharSequence>()) }
        assertEquals("Title", header.captured.title.toString())
        assertNull(header.captured.startHeaderAction)
    }

    @Test
    fun `message template uses the legacy setters below Car API 7`() {
        apiLevel = 6
        val builder = mockk<MessageTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", startAction = null)

        verify(exactly = 0) { builder.setHeader(any()) }
        verify { builder.setTitle("Title") }
        verify(exactly = 0) { builder.setHeaderAction(any()) }
    }

    @Test
    fun `list template shows the same title and back on every Car API level`() {
        listOf(3, 6, 7).forEach { level ->
            apiLevel = level

            val template = ListTemplate.Builder()
                .setLoading(true)
                .setHeaderCompat(carContext, "Title", Action.BACK)
                .build()

            assertEquals("Title", template.header!!.title.toString())
            assertEquals(Action.BACK, template.header!!.startHeaderAction)
        }
    }

    @Test
    fun `list template uses only the legacy setters at Car API 5`() {
        apiLevel = 5
        val builder = mockk<ListTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, "Title", Action.BACK)

        verify(exactly = 0) { builder.setHeader(any()) }
        verify { builder.setTitle("Title") }
        verify { builder.setHeaderAction(Action.BACK) }
    }

    @Test
    fun `place list template uses the header and the legacy setters from Car API 5`() {
        listOf(5, 6, 7, 8).forEach { level ->
            apiLevel = level

            val template = placeListTemplate()

            assertEquals(Action.BACK, template.header!!.startHeaderAction)
            assertNull(template.header!!.title)
            assertNull(template.title)
            assertEquals(Action.BACK, template.headerAction)
        }
    }

    @Test
    fun `place list template uses only the legacy setters below Car API 5`() {
        listOf(3, 4).forEach { level ->
            apiLevel = level

            val template = placeListTemplate()

            assertNull(template.header)
            assertNull(template.title)
            assertEquals(Action.BACK, template.headerAction)
        }
    }

    @Test
    fun `route preview template uses the header and the legacy setters from Car API 5`() {
        listOf(5, 6, 7, 8).forEach { level ->
            apiLevel = level

            val template = routePreviewTemplate()

            assertEquals("Title", template.header!!.title.toString())
            assertEquals(Action.BACK, template.header!!.startHeaderAction)
            assertEquals("Title", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
        }
    }

    @Test
    fun `route preview template uses only the legacy setters below Car API 5`() {
        listOf(3, 4).forEach { level ->
            apiLevel = level

            val template = routePreviewTemplate()

            assertNull(template.header)
            assertEquals("Title", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
        }
    }

    // Header.Builder.build() throws without a non-empty title or a start action.

    @Test
    fun `no header is set without a title and a start action`() {
        listOf(3, 5, 7).forEach { level ->
            apiLevel = level

            val message = MessageTemplate.Builder("Message")
                .setHeaderCompat(carContext, title = null, startAction = null)
                .build()
            val routePreview = RoutePreviewNavigationTemplate.Builder()
                .setLoading(true)
                .setHeaderCompat(carContext, title = null, startAction = null)
                .build()

            assertNull(message.header)
            assertNull(message.title)
            assertNull(routePreview.header)
            assertNull(routePreview.title)
            assertNull(routePreview.headerAction)
        }
    }

    @Test
    fun `an empty title alone sets no header and no legacy title`() {
        listOf(3, 5, 7).forEach { level ->
            apiLevel = level

            val message = MessageTemplate.Builder("Message")
                .setHeaderCompat(carContext, title = "", startAction = null)
                .build()
            val routePreview = RoutePreviewNavigationTemplate.Builder()
                .setLoading(true)
                .setHeaderCompat(carContext, title = "", startAction = null)
                .build()

            assertNull(message.header)
            assertNull(message.title)
            assertNull(routePreview.header)
            assertNull(routePreview.title)
        }
    }

    @Test
    fun `an empty title is left out of a header with a start action`() {
        apiLevel = 7
        val builder = mockk<ListTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, title = "", startAction = Action.BACK)

        val header = slot<Header>()
        verify { builder.setHeader(capture(header)) }
        assertNull(header.captured.title)
        assertEquals(Action.BACK, header.captured.startHeaderAction)
    }

    @Test
    fun `an empty title is left out of the legacy fields`() {
        apiLevel = 6
        val builder = mockk<ListTemplate.Builder>(relaxed = true)

        builder.setHeaderCompat(carContext, title = "", startAction = Action.BACK)

        verify(exactly = 0) { builder.setTitle(any<CharSequence>()) }
        verify { builder.setHeaderAction(Action.BACK) }
    }

    private fun placeListTemplate() = PlaceListNavigationTemplate.Builder()
        .setLoading(true)
        .setHeaderCompat(carContext, title = null, startAction = Action.BACK)
        .build()

    private fun routePreviewTemplate() = RoutePreviewNavigationTemplate.Builder()
        .setLoading(true)
        .setHeaderCompat(carContext, "Title", Action.BACK)
        .build()
}

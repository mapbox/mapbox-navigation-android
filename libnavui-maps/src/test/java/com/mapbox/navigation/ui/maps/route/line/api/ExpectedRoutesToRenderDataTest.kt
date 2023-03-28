package com.mapbox.navigation.ui.maps.route.line.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpectedRoutesToRenderDataTest {

    private val sut = ExpectedRoutesToRenderData()

    @Test
    fun allRenderedRouteIds_empty() {
        assertEquals(0, sut.allRenderedRouteIds.size)
    }

    @Test
    fun allClearedRouteIds_empty() {
        assertEquals(0, sut.allClearedRouteIds.size)
    }

    @Test
    fun isEmpty_empty() {
        assertTrue(sut.isEmpty())
    }

    @Test
    fun getRenderedRouteId_empty() {
        assertNull(sut.getRenderedRouteId("source1"))
    }

    @Test
    fun getClearedRouteId_empty() {
        assertNull(sut.getClearedRouteId("source1"))
    }

    @Test
    fun getSourceIdAndDataId_empty() {
        assertEquals(0, sut.getSourceAndDataIds().size)
    }

    @Test
    fun allRenderedRouteIds_singleElement() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")
        sut.addClearedRoute("source2", 3, "id#2")

        assertEquals(setOf("id#0"), sut.allRenderedRouteIds)
    }

    @Test
    fun allClearedRouteIds_singleElement() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")
        sut.addRenderedRoute("source2", 3, "id#2")

        assertEquals(setOf("id#0"), sut.allClearedRouteIds)
    }

    @Test
    fun isEmpty_singleRenderedElement() {
        sut.addRenderedRoute("source1", 1, "id#0")

        assertFalse(sut.isEmpty())
    }

    @Test
    fun isEmpty_singleClearedElement() {
        sut.addClearedRoute("source1", 1, "id#0")

        assertFalse(sut.isEmpty())
    }

    @Test
    fun getRenderedRouteId_singleElement() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")
        sut.addClearedRoute("source2", 3, "id#2")

        assertEquals("id#0", sut.getRenderedRouteId("source1"))
    }

    @Test
    fun getClearedRouteId_singleElement() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")
        sut.addRenderedRoute("source2", 3, "id#2")

        assertEquals("id#0", sut.getClearedRouteId("source1"))
    }

    @Test
    fun getSourceIdAndDataId_singleRenderedElement() {
        sut.addRenderedRoute("source1", 1, "id#0")

        assertEquals(listOf("source1" to 1), sut.getSourceAndDataIds())
    }

    @Test
    fun getSourceIdAndDataId_singleClearedElement() {
        sut.addClearedRoute("source1", 1, "id#0")

        assertEquals(listOf("source1" to 1), sut.getSourceAndDataIds())
    }

    @Test
    fun allRenderedRouteIds_singleRenderedElementAddedMultipleTimes() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")

        assertEquals(setOf("id#1"), sut.allRenderedRouteIds)
    }

    @Test
    fun allClearedRouteIds_singleClearedElementAddedMultipleTimes() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")

        assertEquals(setOf("id#1"), sut.allClearedRouteIds)
    }

    @Test
    fun isEmpty_singleRenderedElementAddedMultipleTimes() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")

        assertFalse(sut.isEmpty())
    }

    @Test
    fun isEmpty_singleClearedElementAddedMultipleTimes() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")

        assertFalse(sut.isEmpty())
    }

    @Test
    fun getRenderedRouteId_singleElementAddedMultipleTimes() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")

        assertEquals("id#1", sut.getRenderedRouteId("source1"))
    }

    @Test
    fun getClearedRouteId_singleElementAddedMultipleTimes() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")

        assertEquals("id#1", sut.getClearedRouteId("source1"))
    }

    @Test
    fun getSourceIdAndDataId_singleRenderedElementAddedMultipleTimes() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 2, "id#0")
        sut.addRenderedRoute("source1", 2, "id#1")

        assertEquals(listOf("source1" to 2), sut.getSourceAndDataIds())
    }

    @Test
    fun getSourceIdAndDataId_singleClearedElementAddedMultipleTimes() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 2, "id#0")
        sut.addClearedRoute("source1", 2, "id#1")

        assertEquals(listOf("source1" to 2), sut.getSourceAndDataIds())
    }

    @Test
    fun allRenderedRouteIds_multipleElements() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source1", 10, "id#3")
        sut.addClearedRoute("source2", 3, "id#5")
        sut.addClearedRoute("source3", 2, "id#4")
        sut.addRenderedRoute("source2", 1, "id#1")

        assertEquals(setOf("id#0", "id#1"), sut.allRenderedRouteIds)
    }

    @Test
    fun allClearedRouteIds_multipleElements() {
        sut.addClearedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source1", 10, "id#3")
        sut.addRenderedRoute("source2", 3, "id#5")
        sut.addClearedRoute("source2", 1, "id#1")
        sut.addRenderedRoute("source3", 2, "id#4")

        assertEquals(setOf("id#0", "id#1"), sut.allClearedRouteIds)
    }

    @Test
    fun isEmpty_multipleElements() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source2", 1, "id#1")

        assertFalse(sut.isEmpty())
    }

    @Test
    fun geRouteId_multipleElements() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addRenderedRoute("source2", 1, "id#1")
        sut.addClearedRoute("source2", 2, "id#3")
        sut.addClearedRoute("source3", 4, "id#5")
        sut.addClearedRoute("source10", 5, "id#7")

        assertEquals("id#0", sut.getRenderedRouteId("source1"))
        assertEquals("id#1", sut.getRenderedRouteId("source2"))
        assertNull(sut.getRenderedRouteId("source3"))
        assertNull(sut.getRenderedRouteId("source10"))
        assertEquals("id#3", sut.getClearedRouteId("source2"))
        assertEquals("id#5", sut.getClearedRouteId("source3"))
        assertEquals("id#7", sut.getClearedRouteId("source10"))
        assertNull(sut.getClearedRouteId("source1"))
    }

    @Test
    fun getSourceIdAndDataId_multipleElements() {
        sut.addRenderedRoute("source1", 1, "id#0")
        sut.addClearedRoute("source2", 1, "id#1")
        sut.addRenderedRoute("source3", 2, "id#0")

        assertEquals(
            listOf("source1" to 1, "source3" to 2, "source2" to 1),
            sut.getSourceAndDataIds()
        )
    }
}

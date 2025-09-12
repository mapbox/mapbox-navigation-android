package com.mapbox.navigation.core.navigator.offline

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DownloadedTilesetsFetcherTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `TilesetDescriptorMetadataParser should parse valid JSON correctly`() {
        val json = """
        {
            "resolved": [
                {
                    "domain": "Maps",
                    "version": "",
                    "dataset": "mapbox.mapbox-bathymetry-v2"
                },
                {
                    "domain": "Navigation",
                    "version": "2025_08_31-06_57_13",
                    "dataset": "mapbox/driving-traffic"
                },
                {
                    "domain": "Adas",
                    "version": "2025_08_31-06_57_13",
                    "dataset": "mapbox/driving-traffic"
                }
            ]
        }
        """.trimIndent()

        val result = DownloadedTilesetsFetcher.parseFromJson(json)

        assertEquals("Should parse 3 tileset descriptors", 3, result.size)

        val maps = result[0]
        assertEquals("Maps", maps.domain)
        assertEquals("", maps.version)
        assertEquals("mapbox.mapbox-bathymetry-v2", maps.dataset)
        assertFalse(maps.isAdasDomain || maps.isNavigationDomain)
        assertNull("Empty version should result in null releaseDate", maps.releaseDate)

        val navigation = result[1]
        assertEquals("Navigation", navigation.domain)
        assertEquals("2025_08_31-06_57_13", navigation.version)
        assertEquals("mapbox/driving-traffic", navigation.dataset)
        assertTrue(navigation.isNavigationDomain)
        assertFalse(navigation.isAdasDomain)
        verifyReleaseVersionDate(navigation, "2025_08_31-06_57_13")

        val adas = result[2]
        assertEquals("Adas", adas.domain)
        assertEquals("2025_08_31-06_57_13", adas.version)
        assertEquals("mapbox/driving-traffic", adas.dataset)
        assertTrue(adas.isAdasDomain)
        assertFalse(adas.isNavigationDomain)
        verifyReleaseVersionDate(adas, "2025_08_31-06_57_13")
    }

    @Test
    fun `TilesetDescriptorMetadataParser should handle empty resolved array`() {
        val json = """{"resolved": []}"""

        val result = DownloadedTilesetsFetcher.parseFromJson(json)

        assertEquals("Should return empty list", 0, result.size)
    }

    @Test
    fun `TilesetDescriptorMetadataParser should handle invalid JSON gracefully`() {
        val invalidJson = """{"invalid": "json"}"""

        val result = DownloadedTilesetsFetcher.parseFromJson(invalidJson)

        assertEquals("Should return empty list for invalid JSON", 0, result.size)
    }

    @Test
    fun `TilesetDescriptorMetadataParser should handle null or empty JSON gracefully`() {
        val nullJson = "null"
        val emptyJson = ""
        val whitespaceJson = "   "

        val nullResult = DownloadedTilesetsFetcher.parseFromJson(nullJson)
        val emptyResult = DownloadedTilesetsFetcher.parseFromJson(emptyJson)
        val whitespaceResult = DownloadedTilesetsFetcher.parseFromJson(whitespaceJson)

        assertEquals("Should return empty list for null JSON", 0, nullResult.size)
        assertEquals("Should return empty list for empty JSON", 0, emptyResult.size)
        assertEquals(
            "Should return empty list for whitespace JSON",
            0,
            whitespaceResult.size,
        )
    }

    @Test
    fun `TilesetDescriptorMetadataParser should handle JSON with wrong structure gracefully`() {
        val wrongStructureJson = """{"notResolved": [{"domain": "Test"}]}"""

        val result = DownloadedTilesetsFetcher.parseFromJson(wrongStructureJson)

        assertEquals("Should return empty list for wrong structure JSON", 0, result.size)
    }

    @Test
    fun `TilesetDescriptorMetadataParser should handle JSON with invalid data types gracefully`() {
        val invalidTypesJson = """
        {
            "resolved": [
                {
                    "domain": 123,
                    "version": "",
                    "dataset": "test-dataset",
                    "levels": "not-an-array"
                }
            ]
        }
        """.trimIndent()

        val result = DownloadedTilesetsFetcher.parseFromJson(invalidTypesJson)

        assertEquals("Should return empty list for invalid data types", 0, result.size)
    }

    private companion object {

        private val VERSION_DATE_FORMATTER = SimpleDateFormat(
            "yyyy_MM_dd-HH_mm_ss",
            Locale.US,
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun verifyReleaseVersionDate(metadata: DownloadedTileset, expectedVersionName: String) {
            val formattedDate = VERSION_DATE_FORMATTER.format(metadata.releaseDate!!)
            assertEquals(
                "Parsed version date should match version name",
                expectedVersionName,
                formattedDate,
            )
        }
    }
}

package com.mapbox.navigation.core.navigator.offline

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TilesetReleaseDateParserTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `parseReleaseDate should parse valid version names correctly`() {
        val formatter = SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val testCases = listOf(
            "2025_09_07-12_32_17",
            "2025_08_31-06_57_13",
            "2025_08_24-07_50_48",
            "2025_08_17-07_16_00",
            "2025_08_10-08_17_22",
            "2025_08_03-14_20_15",
            "2024_12_31-23_59_59",
            "2023_01_01-00_00_00",
        )

        testCases.forEach { versionName ->
            val releaseDate = TilesetReleaseDateParser.parseReleaseDate(versionName)

            assertNotNull(
                "Release date should not be null for valid version: $versionName",
                releaseDate,
            )

            // Verify the parsed date matches what we expect by formatting it back
            val expectedFormatted = formatter.format(releaseDate!!)
            assertEquals(
                "Parsed date should match original version name: $versionName",
                versionName,
                expectedFormatted,
            )
        }
    }

    @Test
    fun `parseReleaseDate should return null for incorrect version names`() {
        val incorrectVersionNames = listOf(
            "2025_13_01-12_00_00", // Incorrect month
            "2025_02_32-12_00_00", // Incorrect day
            "2025_01_01-25_00_00", // Incorrect hour
            "2025_01_01-12_60_00", // Incorrect minute
            "2025_01_01-12_00_60", // Incorrect second
            "2025-01-01T12:00:00", // Wrong format
            "2025_01_01", // Missing time part
            "12_00_00", // Missing date part
            "incorrect_format", // Not a date format
            "", // Empty version
        )

        incorrectVersionNames.forEach { versionName ->
            val releaseDate = TilesetReleaseDateParser.parseReleaseDate(versionName)
            assertNull(
                "Release date should be null for incorrect version: $versionName",
                releaseDate,
            )
        }
    }
}

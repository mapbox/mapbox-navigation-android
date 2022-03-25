package com.mapbox.navigation.dropin.extensions

import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.NavigationStyles
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxStyleExtensionsKtTest {

    // Map of (URI to EXPECTED_STYLE_ID)
    private val fixtures: Map<String, String?> = mapOf(
        NavigationStyles.NAVIGATION_NIGHT_STYLE to NavigationStyles.NAVIGATION_NIGHT_STYLE_ID,
        "mapbox://styles/mapbox/dark-v10" to "dark-v10",
        "mapbox://styles/mapbox/navigation-day-v1" to "navigation-day-v1",
        "mapbox://styles/mapbox/mnuhbadfvads803124" to "mnuhbadfvads803124",
        "mapbox://styles/user/styleId" to "styleId",
        "mapbox://styles/some-user-id/custom-style-2123" to "custom-style-2123",
        "http://asdasd/asdas" to null,
        "some random string" to null
    )

    @Test
    fun `getStyleId should return STYLE_ID`() {
        fixtures.forEach { (uri, expectedId) ->
            val style = mockk<Style> {
                every { styleURI } returns uri
            }
            assertEquals(expectedId, style.getStyleId())
        }
    }
}

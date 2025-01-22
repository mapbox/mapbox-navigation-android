package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.NavigationStyles
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxStyleExtensionsKtTest {

    // List of (URI, EXPECTED_USER_ID, EXPECTED_STYLE_ID)
    private val fixtures = listOf(
        Triple(
            NavigationStyles.NAVIGATION_NIGHT_STYLE,
            NavigationStyles.NAVIGATION_NIGHT_STYLE_USER_ID,
            NavigationStyles.NAVIGATION_NIGHT_STYLE_ID,
        ),
        Triple("mapbox://styles/mapbox/dark-v10", "mapbox", "dark-v10"),
        Triple("mapbox://styles/mapbox/navigation-day-v1", "mapbox", "navigation-day-v1"),
        Triple("mapbox://styles/mapbox/mnuhbadfvads803124", "mapbox", "mnuhbadfvads803124"),
        Triple("mapbox://styles/user/styleId", "user", "styleId"),
        Triple(
            "mapbox://styles/some-user-id/custom-style-2123",
            "some-user-id",
            "custom-style-2123",
        ),
        Triple("http://asdasd/asdas", null, null),
        Triple("some random string", null, null),
    )

    @Test
    fun `getUserId should return USER_ID`() {
        for ((uri, userId, _) in fixtures) {
            val style = mockk<Style> {
                every { styleURI } returns uri
            }
            assertEquals(userId, style.getUserId())
        }
    }

    @Test
    fun `getStyleId should return STYLE_ID`() {
        for ((uri, _, styleId) in fixtures) {
            val style = mockk<Style> {
                every { styleURI } returns uri
            }
            assertEquals(styleId, style.getStyleId())
        }
    }
}

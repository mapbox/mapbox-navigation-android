package com.mapbox.navigation.ui.androidauto.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MapUserStyleObserverTest {

    @Test
    fun `mapbox style uri is split into user and style ids`() {
        assertEquals("user" to "style", parseStyleUri("mapbox://styles/user/style"))
    }

    @Test
    fun `query parameters are not part of the style id`() {
        assertEquals("user" to "style", parseStyleUri("mapbox://styles/user/style?fresh=true"))
    }

    @Test
    fun `draft style path stays part of the style id`() {
        assertEquals("user" to "style/draft", parseStyleUri("mapbox://styles/user/style/draft"))
    }

    @Test
    fun `mapbox style uri without a style id produces empty ids`() {
        assertEquals("" to "", parseStyleUri("mapbox://styles/user"))
        assertEquals("" to "", parseStyleUri("mapbox://styles/"))
    }

    @Test
    fun `non mapbox style uris produce empty ids`() {
        assertEquals("" to "", parseStyleUri(""))
        assertEquals("" to "", parseStyleUri("https://example.com/style.json"))
        assertEquals("" to "", parseStyleUri("asset://style.json"))
    }
}

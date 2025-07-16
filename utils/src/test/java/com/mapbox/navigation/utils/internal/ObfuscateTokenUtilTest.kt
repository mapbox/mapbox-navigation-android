package com.mapbox.navigation.utils.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ObfuscateTokenUtilTest {

    @Test
    fun `obfuscateAccessToken should redact the token in a simple url`() {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
            "-73.989,40.733;-74,40.733?access_token=pk.1234567890"
        val expected = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
            "-73.989,40.733;-74,40.733?access_token=****7890"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should work when token is the only parameter`() {
        val url = "https://api.mapbox.com?access_token=pk.1234567890"
        val expected = "https://api.mapbox.com?access_token=****7890"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should work with other query parameters`() {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving?" +
            "geometries=polyline&access_token=pk.1234567890&voice_instructions=true"
        val expected = "https://api.mapbox.com/directions/v5/mapbox/driving?" +
            "geometries=polyline&access_token=****7890&voice_instructions=true"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should not change string without access token`() {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving"
        assertEquals(url, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle empty access token`() {
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving?access_token="
        assertEquals(url, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle multiple access tokens`() {
        val url = "https://api.mapbox.com?access_token=pk.first&param=1&access_token=pk.second"
        val expected = "https://api.mapbox.com?access_token=****irst&param=1&access_token=****cond"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle empty string`() {
        val url = ""
        assertEquals(url, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle token shorter than 4 chars`() {
        val url = "https://api.mapbox.com?access_token=123"
        val expected = "https://api.mapbox.com?access_token=****123"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle token with exactly 4 chars`() {
        val url = "https://api.mapbox.com?access_token=1234"
        val expected = "https://api.mapbox.com?access_token=****1234"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle token with more than 4 chars`() {
        val url = "https://api.mapbox.com?access_token=12345"
        val expected = "https://api.mapbox.com?access_token=****2345"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle token ending with newline`() {
        val url = "https://api.mapbox.com?access_token=pk.1234567890\n"
        val expected = "https://api.mapbox.com?access_token=****7890\n"
        assertEquals(expected, url.obfuscateAccessToken())
    }

    @Test
    fun `obfuscateAccessToken should handle token followed by ampersand`() {
        val url = "https://api.mapbox.com?access_token=pk.1234567890&other=param"
        val expected = "https://api.mapbox.com?access_token=****7890&other=param"
        assertEquals(expected, url.obfuscateAccessToken())
    }
}

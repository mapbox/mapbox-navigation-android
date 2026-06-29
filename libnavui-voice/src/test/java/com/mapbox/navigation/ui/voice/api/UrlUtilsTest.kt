package com.mapbox.navigation.ui.voice.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun `ampersand in plain text is percent encoded`() {
        val encoded = UrlUtils.encodePathSegment("Barnes & Noble")
        assertTrue("& must be percent-encoded as %26", encoded.contains("%26"))
        assertFalse("raw & must not appear in path segment", encoded.contains("&"))
    }

    @Test
    fun `slash in text is percent encoded`() {
        val encoded = UrlUtils.encodePathSegment("Take US-101/I-5")
        assertTrue("/ must be percent-encoded as %2F", encoded.contains("%2F"))
        assertFalse(encoded.contains("/"))
    }

    @Test
    fun `space is percent encoded`() {
        assertEquals("Turn%20left", UrlUtils.encodePathSegment("Turn left"))
    }

    @Test
    fun `plain ascii without reserved characters is unchanged`() {
        assertEquals("voice", UrlUtils.encodePathSegment("voice"))
    }
}

package com.mapbox.navigation.route.internal.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpUrlExTest {

    @Test
    fun `query param redacted if present, string`() {
        val input = "https://test.com/?query=1&$ACCESS_TOKEN_QUERY_PARAM=pk.123"
        val expected = "https://test.com/?query=1&$ACCESS_TOKEN_QUERY_PARAM=$REDACTED"

        val result = input.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)

        assertEquals(expected, result)
    }

    @Test
    fun `query param redacted if present, HttpUrl`() {
        val input = "https://test.com/?$ACCESS_TOKEN_QUERY_PARAM=pk.123".toHttpUrl()
        val expected = "https://test.com/?$ACCESS_TOKEN_QUERY_PARAM=$REDACTED".toHttpUrl()

        val result = input.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)

        assertEquals(expected, result)
    }

    @Test
    fun `query param not redacted if not present, string`() {
        val input = "https://test.com/?query=1"

        val result = input.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)

        assertEquals(input, result)
    }

    @Test
    fun `query param not redacted if not present, HttpUrl`() {
        val input = "https://test.com/".toHttpUrl()

        val result = input.redactQueryParam(ACCESS_TOKEN_QUERY_PARAM)

        assertEquals(input, result)
    }
}

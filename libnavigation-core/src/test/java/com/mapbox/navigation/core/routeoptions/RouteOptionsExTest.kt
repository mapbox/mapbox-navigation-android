package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.navigation.testing.factories.createRouteOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteOptionsExTest {

    @Test
    fun `isEVRoute null unrecognizedJsonProperties`() {
        val options = createRouteOptions(unrecognizedProperties = null)

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `isEVRoute empty unrecognizedJsonProperties`() {
        val options = createRouteOptions(unrecognizedProperties = emptyMap())

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `isEVRoute no engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("aaa" to JsonPrimitive("bbb"))
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `isEVRoute non-string engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonObject())
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `isEVRoute non-electric engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonPrimitive("non-electric"))
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `isEVRoute electric engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonPrimitive("electric"))
        )

        assertTrue(options.isEVRoute())
    }
}

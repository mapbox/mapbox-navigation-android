package com.mapbox.navigation.core.routeoptions

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.navigation.testing.factories.createRouteOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteOptionsExTest {

    @Test
    fun `RouteOptions isEVRoute null unrecognizedJsonProperties`() {
        val options = createRouteOptions(unrecognizedProperties = null)

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `RouteOptions isEVRoute empty unrecognizedJsonProperties`() {
        val options = createRouteOptions(unrecognizedProperties = emptyMap())

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `RouteOptions isEVRoute no engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("aaa" to JsonPrimitive("bbb")),
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `RouteOptions isEVRoute non-string engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonObject()),
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `RouteOptions isEVRoute non-electric engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonPrimitive("non-electric")),
        )

        assertFalse(options.isEVRoute())
    }

    @Test
    fun `RouteOptions isEVRoute electric engine`() {
        val options = createRouteOptions(
            unrecognizedProperties = mapOf("engine" to JsonPrimitive("electric")),
        )

        assertTrue(options.isEVRoute())
    }

    @Test
    fun `map isEVRoute null`() {
        val map: Map<String, JsonElement>? = null

        assertFalse(map.isEVRoute())
    }

    @Test
    fun `map isEVRoute empty`() {
        val map = emptyMap<String, JsonElement>()

        assertFalse(map.isEVRoute())
    }

    @Test
    fun `map isEVRoute no engine`() {
        val map = mapOf("aaa" to JsonPrimitive("bbb"))

        assertFalse(map.isEVRoute())
    }

    @Test
    fun `map isEVRoute non-string engine`() {
        val map = mapOf("engine" to JsonObject())

        assertFalse(map.isEVRoute())
    }

    @Test
    fun `map isEVRoute non-electric engine`() {
        val map = mapOf("engine" to JsonPrimitive("non-electric"))

        assertFalse(map.isEVRoute())
    }

    @Test
    fun `map isEVRoute electric engine`() {
        val map = mapOf("engine" to JsonPrimitive("electric"))

        assertTrue(map.isEVRoute())
    }
}

package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlexBuffers
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlatbuffersMapWrapperTest {

    @Test
    fun `get returns null when flexMap is null`() {
        val result = FlatbuffersMapWrapper.get(null) {
            object : Map.Entry<String, String> {
                override val key: String = "key"
                override val value: String = "value"
            }
        }

        assertNull(result)
    }

    @Test
    fun `get returns null when flexMap size is zero`() {
        val flexMap = createMockFlexMap(size = 0)
        val result = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, String> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: String = flexMap.get(index).toString()
            }
        }

        assertNull(result)
    }

    @Test
    fun `non-caching wrapper returns correct size`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertEquals(3, map?.size)
    }

    @Test
    fun `non-caching wrapper get returns correct value`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertEquals(0, map?.get("key0"))
        assertEquals(10, map?.get("key1"))
        assertEquals(20, map?.get("key2"))
        assertNull(map?.get("key3"))
    }

    @Test
    fun `non-caching wrapper containsKey returns correct result`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertTrue(map?.containsKey("key0") ?: false)
        assertTrue(map?.containsKey("key1") ?: false)
        assertTrue(map?.containsKey("key2") ?: false)
        assertFalse(map?.containsKey("key3") ?: true)
    }

    @Test
    fun `non-caching wrapper containsValue returns correct result`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertTrue(map?.containsValue(0) ?: false)
        assertTrue(map?.containsValue(10) ?: false)
        assertTrue(map?.containsValue(20) ?: false)
        assertFalse(map?.containsValue(30) ?: true)
    }

    @Test
    fun `non-caching wrapper entries iterator works correctly`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        val entries = map?.entries?.toList() ?: emptyList()
        assertEquals(3, entries.size)
        assertEquals("key0", entries[0].key)
        assertEquals(0, entries[0].value)
        assertEquals("key1", entries[1].key)
        assertEquals(10, entries[1].value)
        assertEquals("key2", entries[2].key)
        assertEquals(20, entries[2].value)
    }

    @Test
    fun `non-caching wrapper keys iterator works correctly`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        val keys = map?.keys?.toList() ?: emptyList()
        assertEquals(3, keys.size)
        assertTrue(keys.contains("key0"))
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `non-caching wrapper values iterator works correctly`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        val values = map?.values?.toList() ?: emptyList()
        assertEquals(3, values.size)
        assertTrue(values.contains(0))
        assertTrue(values.contains(10))
        assertTrue(values.contains(20))
    }

    @Test
    fun `non-caching wrapper calls flexMap on every access`() {
        val flexMap = createMockFlexMap(size = 2)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        // First access
        map?.get("key0")

        // Second access - should call flexMap again (non-caching)
        map?.get("key0")

        // Third access through entries - should call flexMap again
        map?.entries?.iterator()?.next()

        verify(exactly = 3) { flexMap.get(0) }
    }

    @Test
    fun `non-caching wrapper only accesses requested entries`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        // Access only key0
        assertEquals(0, map?.get("key0"))

        // Verify flexMap.get(0) was called, but not get(1) or get(2)
        verify(atLeast = 1) { flexMap.get(0) }
        verify(exactly = 0) { flexMap.get(1) }
        verify(exactly = 0) { flexMap.get(2) }
    }

    @Test
    fun `caching wrapper returns correct size`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertEquals(3, map?.size)
    }

    @Test
    fun `caching wrapper get returns correct value`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertEquals(0, map?.get("key0"))
        assertEquals(10, map?.get("key1"))
        assertEquals(20, map?.get("key2"))
        assertNull(map?.get("key3"))
    }

    @Test
    fun `caching wrapper containsKey returns correct result`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertTrue(map?.containsKey("key0") ?: false)
        assertTrue(map?.containsKey("key1") ?: false)
        assertTrue(map?.containsKey("key2") ?: false)
        assertFalse(map?.containsKey("key3") ?: true)
    }

    @Test
    fun `caching wrapper containsValue returns correct result`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        assertTrue(map?.containsValue(0) ?: false)
        assertTrue(map?.containsValue(10) ?: false)
        assertTrue(map?.containsValue(20) ?: false)
        assertFalse(map?.containsValue(30) ?: true)
    }

    @Test
    fun `caching wrapper entries iterator works correctly`() {
        val flexMap = createMockFlexMap(size = 3)
        val map = FlatbuffersMapWrapper.get(flexMap) { index ->
            object : Map.Entry<String, Int> {
                override val key: String = flexMap.keys().get(index).toString()
                override val value: Int = flexMap.get(index).asInt()
            }
        }

        val entries = map?.entries?.toList() ?: emptyList()
        assertEquals(3, entries.size)
        assertEquals("key0", entries[0].key)
        assertEquals(0, entries[0].value)
        assertEquals("key1", entries[1].key)
        assertEquals(10, entries[1].value)
        assertEquals("key2", entries[2].key)
        assertEquals(20, entries[2].value)
    }

    private fun createMockFlexMap(size: Int): FlexBuffers.Map {
        val keyMocks = (0 until size).map { index ->
            mockk<FlexBuffers.Key>(relaxed = true) {
                every { this@mockk.toString() } returns "key$index"
            }
        }

        val keys = mockk<FlexBuffers.KeyVector>(relaxed = true) {
            every { size() } returns size
            every { get(any<Int>()) } answers {
                val index = firstArg<Int>()
                keyMocks[index]
            }
        }

        val valueMocks = (0 until size).map { index ->
            mockk<FlexBuffers.Reference>(relaxed = true) {
                every { asInt() } returns index * 10
            }
        }

        val flexMap = mockk<FlexBuffers.Map>(relaxed = true) {
            every { size() } returns size
            every { keys() } returns keys
            every { get(any<Int>()) } answers {
                val index = firstArg<Int>()
                valueMocks[index]
            }
        }

        return flexMap
    }
}

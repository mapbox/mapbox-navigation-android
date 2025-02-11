package com.mapbox.navigation.utils.internal

import com.mapbox.common.NativeLoggerWrapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoggerProviderFormatTest {

    @Before
    fun setup() {
        mockkObject(NativeLoggerWrapper)
        every { NativeLoggerWrapper.info(any(), any()) } just Runs
        every { NativeLoggerWrapper.warning(any(), any()) } just Runs
        every { NativeLoggerWrapper.debug(any(), any()) } just Runs
        every { NativeLoggerWrapper.error(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(NativeLoggerWrapper)
    }

    @Test
    fun `format log V - without category`() {
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.debug(capture(slotMessage), capture(slotTag)) } just Runs

        logV("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
    }

    @Test
    fun `format log V - with category`() {
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.debug(capture(slotMessage), capture(slotCategory)) } just Runs

        logV("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
    }

    @Test
    fun `format log D - without category`() {
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.debug(capture(slotMessage), capture(slotTag)) } just Runs

        logV("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
    }

    @Test
    fun `format log D - with category`() {
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.debug(capture(slotMessage), capture(slotCategory)) } just Runs

        logD("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
    }

    @Test
    fun `format log I - without category`() {
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.info(capture(slotMessage), capture(slotTag)) } just Runs

        logI("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
    }

    @Test
    fun `format log I - with category`() {
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.info(capture(slotMessage), capture(slotCategory)) } just Runs

        logI("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
    }

    @Test
    fun `format log W - without category`() {
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.warning(capture(slotMessage), capture(slotTag)) } just Runs

        logW("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
    }

    @Test
    fun `format log W - with category`() {
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.warning(capture(slotMessage), capture(slotCategory)) } just Runs

        logW("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
    }

    @Test
    fun `format log E - without category`() {
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.error(capture(slotMessage), capture(slotTag)) } just Runs

        logE("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
    }

    @Test
    fun `format log E - with category`() {
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { NativeLoggerWrapper.error(capture(slotMessage), capture(slotCategory)) } just Runs

        logE("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
    }
}

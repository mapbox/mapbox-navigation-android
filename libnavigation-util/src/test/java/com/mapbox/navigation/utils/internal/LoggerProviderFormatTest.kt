package com.mapbox.navigation.utils.internal

import com.mapbox.common.Logger
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerProviderFormatTest {

    @Test
    fun `format log V - without category`() {
        mockkStatic(Logger::class)
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.d(capture(slotTag), capture(slotMessage)) } just Runs

        logV("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log V - with category`() {
        mockkStatic(Logger::class)
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.d(capture(slotCategory), capture(slotMessage)) } just Runs

        logV("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log D - without category`() {
        mockkStatic(Logger::class)
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.d(capture(slotTag), capture(slotMessage)) } just Runs

        logV("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log D - with category`() {
        mockkStatic(Logger::class)
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.d(capture(slotCategory), capture(slotMessage)) } just Runs

        logD("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log I - without category`() {
        mockkStatic(Logger::class)
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.i(capture(slotTag), capture(slotMessage)) } just Runs

        logI("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log I - with category`() {
        mockkStatic(Logger::class)
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.i(capture(slotCategory), capture(slotMessage)) } just Runs

        logI("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log W - without category`() {
        mockkStatic(Logger::class)
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.w(capture(slotTag), capture(slotMessage)) } just Runs

        logW("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log W - with category`() {
        mockkStatic(Logger::class)
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.w(capture(slotCategory), capture(slotMessage)) } just Runs

        logW("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log E - without category`() {
        mockkStatic(Logger::class)
        val slotTag = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.e(capture(slotTag), capture(slotMessage)) } just Runs

        logE("any_message")

        assertEquals("nav-sdk", slotTag.captured)
        assertEquals("any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }

    @Test
    fun `format log E - with category`() {
        mockkStatic(Logger::class)
        val slotCategory = slot<String>()
        val slotMessage = slot<String>()
        every { Logger.e(capture(slotCategory), capture(slotMessage)) } just Runs

        logE("any_message", "any_clz_name")

        assertEquals("nav-sdk", slotCategory.captured)
        assertEquals("[any_clz_name] any_message", slotMessage.captured)
        unmockkStatic(Logger::class)
    }
}

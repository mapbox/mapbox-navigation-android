package com.mapbox.navigation.core.internal.dump

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.FileDescriptor
import java.io.PrintWriter

class MapboxDumpHandlerTest {

    private val fd: FileDescriptor = mockk()
    private val writer: PrintWriter = mockk(relaxed = true)
    private val sut = MapboxDumpHandler()

    @Before
    fun setup() {
        mockkObject(MapboxDumpRegistry)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should use default interceptor when the command is not recognized`() {
        val defaultInterceptor: MapboxDumpInterceptor = mockk(relaxed = true) {
            every { command() } returns "help"
        }
        every { MapboxDumpRegistry.defaultInterceptor } returns defaultInterceptor
        every { MapboxDumpRegistry.getInterceptors("test_command") } returns emptyList()

        sut.handle(fd, writer, arrayOf("test_command"))

        verify { defaultInterceptor.intercept(any(), any(), listOf()) }
    }

    @Test
    fun `should handle a basic command`() {
        val interceptor = mockk<MapboxDumpInterceptor> {
            every { command() } returns "test_command"
            every { intercept(any(), any(), any()) } just runs
        }
        every {
            MapboxDumpRegistry.getInterceptors("test_command")
        } returns listOf(interceptor)

        sut.handle(fd, writer, arrayOf("test_command"))

        verify { interceptor.intercept(any(), any(), listOf("test_command")) }
    }

    @Test
    fun `should call intercept on interceptors with the command`() {
        val interceptorOne = mockk<MapboxDumpInterceptor> {
            every { command() } returns "test_command"
            every { intercept(any(), any(), any()) } just runs
        }
        val interceptorTwo = mockk<MapboxDumpInterceptor> {
            every { command() } returns "not_called"
            every { intercept(any(), any(), any()) } just runs
        }
        val interceptorThree = mockk<MapboxDumpInterceptor> {
            every { command() } returns "test_command"
            every { intercept(any(), any(), any()) } just runs
        }
        every { MapboxDumpRegistry.getInterceptors("test_command") } returns listOf(
            interceptorOne,
            interceptorThree,
        )
        every { MapboxDumpRegistry.getInterceptors("not_called") } returns listOf(
            interceptorTwo,
        )

        sut.handle(fd, writer, arrayOf("test_command"))

        verifyOrder {
            interceptorOne.intercept(any(), any(), listOf("test_command"))
            interceptorThree.intercept(any(), any(), listOf("test_command"))
        }
        verify(exactly = 0) { interceptorTwo.intercept(any(), any(), any()) }
    }

    @Test
    fun `should pass multiple arguments to interceptors`() {
        val interceptorOne = mockk<MapboxDumpInterceptor>(relaxed = true) {
            every { command() } returns "system"
            every { intercept(any(), any(), any()) } just runs
        }
        val interceptorTwo = mockk<MapboxDumpInterceptor> {
            every { command() } returns "feature"
            every { intercept(any(), any(), any()) } just runs
        }
        val interceptorThree = mockk<MapboxDumpInterceptor> {
            every { command() } returns "system"
            every { intercept(any(), any(), any()) } just runs
        }
        every { MapboxDumpRegistry.getInterceptors("system") } returns listOf(
            interceptorOne,
            interceptorThree,
        )
        every { MapboxDumpRegistry.getInterceptors("feature") } returns listOf(
            interceptorTwo,
        )

        val args = arrayOf(
            "system",
            "feature:longitude:-122.523667",
            "feature:latitude:37.975391",
            "system:argument",
            "feature:replay:true",
            "system:value:10",
        )
        sut.handle(fd, writer, args)

        val expectedSystem = listOf("system", "system:argument", "system:value:10")
        val expectedFeature = listOf(
            "feature:longitude:-122.523667",
            "feature:latitude:37.975391",
            "feature:replay:true",
        )
        verify {
            interceptorOne.intercept(any(), any(), expectedSystem)
            interceptorTwo.intercept(any(), any(), expectedFeature)
            interceptorThree.intercept(any(), any(), expectedSystem)
        }
    }
}

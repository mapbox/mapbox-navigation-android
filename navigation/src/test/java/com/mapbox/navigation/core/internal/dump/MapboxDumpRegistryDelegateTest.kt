package com.mapbox.navigation.core.internal.dump

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * This is a singleton that allows downstream solutions to define their own interceptors.
 */
class MapboxDumpRegistryDelegateTest {
    private val sut = MapboxDumpRegistryDelegate()

    @Test
    fun `defaultInterceptor is a help message`() {
        assertTrue(sut.defaultInterceptor is HelpDumpInterceptor)
    }

    @Test
    fun `getInterceptors will return the defaultInterceptor`() {
        assertTrue(sut.getInterceptors().first() is HelpDumpInterceptor)
        assertTrue(sut.getInterceptors("help").first() is HelpDumpInterceptor)
    }

    @Test
    fun `defaultInterceptor can be removed`() {
        sut.defaultInterceptor = null

        assertTrue(sut.getInterceptors().isEmpty())
    }

    @Test
    fun `removeInterceptors can remove the defaultInterceptor`() {
        val helpInterceptor = sut.defaultInterceptor!!
        sut.removeInterceptors(helpInterceptor)

        assertNull(sut.defaultInterceptor)
    }

    @Test
    fun `addInterceptors will result in new getInterceptors`() {
        val interceptorOne = mockk<MapboxDumpInterceptor>()
        val interceptorTwo = mockk<MapboxDumpInterceptor>()

        sut.addInterceptors(interceptorOne, interceptorTwo)
        val interceptors = sut.getInterceptors()

        assertTrue(interceptors.containsAll(listOf(interceptorOne, interceptorTwo)))
    }

    @Test
    fun `getInterceptors can be filtered by command`() {
        val interceptorOne = mockk<MapboxDumpInterceptor> {
            every { command() } returns "system"
        }
        val interceptorTwo = mockk<MapboxDumpInterceptor> {
            every { command() } returns "feature"
        }
        val interceptorThree = mockk<MapboxDumpInterceptor> {
            every { command() } returns "system"
        }

        sut.addInterceptors(interceptorOne, interceptorTwo, interceptorThree)
        val featureInterceptors = sut.getInterceptors("feature")
        val systemInterceptors = sut.getInterceptors("system")

        assertTrue(featureInterceptors.containsAll(listOf(interceptorTwo)))
        assertTrue(systemInterceptors.containsAll(listOf(interceptorOne, interceptorThree)))
    }
}

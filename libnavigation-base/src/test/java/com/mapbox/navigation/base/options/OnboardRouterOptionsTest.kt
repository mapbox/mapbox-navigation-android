package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KClass

class OnboardRouterOptionsTest : BuilderTest<OnboardRouterOptions, OnboardRouterOptions.Builder>() {

    override fun getImplementationClass(): KClass<OnboardRouterOptions> =
        OnboardRouterOptions::class

    override fun getFilledUpBuilder(): OnboardRouterOptions.Builder {
        return OnboardRouterOptions.Builder()
            .filePath("123")
            .tilesUri(mockk(relaxed = true))
            .tilesVersion("456")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    private val validFilePath = "/data/user/0/com.mapbox.navigation.examples/files/" +
        "Offline/api.mapbox.com/2020_02_02-03_00_00/tiles"

    @Test
    fun `filePath should build with defaults`() {
        val onboardRouterOptions = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .build()

        assertNotNull(onboardRouterOptions.tilesUri)
        assertNotNull(onboardRouterOptions.tilesVersion)
        assertNotNull(onboardRouterOptions.filePath)
    }

    @Test
    fun `filePath should successfully build custom file path`() {
        val onboardRouterOptions = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .build()

        assertNotNull(onboardRouterOptions)
    }

    @Test
    fun `tilesUri should fail to build fake uri`() {
        val onboardRouterOptions = try {
            OnboardRouterOptions.Builder()
                .filePath(validFilePath)
                .tilesUri(URI("fake uri"))
        } catch (e: URISyntaxException) {
            null
        }

        assertNull(onboardRouterOptions)
    }

    @Test
    fun `builders should build equal objects`() {
        val options = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .tilesVersion("2020_08_01-03_10_00")
            .build()

        val otherOptions = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .tilesVersion("2020_08_01-03_10_00")
            .build()

        assertEquals(options, otherOptions)
    }

    @Test
    fun `builders should detect u equal objects`() {
        val options = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .tilesVersion("2020_08_01-03_10_00")
            .build()

        val otherOptions = OnboardRouterOptions.Builder()
            .filePath(validFilePath)
            .tilesVersion("2020_08_02-03_10_00")
            .build()

        assertNotEquals(options, otherOptions)
    }
}

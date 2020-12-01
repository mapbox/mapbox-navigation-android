package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KClass

class OnboardRouterOptionsTest : BuilderTest<OnboardRouterOptions, OnboardRouterOptions.Builder>() {

    override fun getImplementationClass(): KClass<OnboardRouterOptions> =
        OnboardRouterOptions::class

    override fun getFilledUpBuilder(): OnboardRouterOptions.Builder {
        return OnboardRouterOptions.Builder()
            .tilesUri(URI("https://my.api.com"))
            .tilesDataset("my_username.osm")
            .tilesProfile("driving")
            .tilesVersion("456")
            .filePath("123")
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test(expected = URISyntaxException::class)
    fun `tilesUri should fail to build fake uri`() {
        OnboardRouterOptions.Builder()
            .tilesUri(URI("fake uri"))
    }
}

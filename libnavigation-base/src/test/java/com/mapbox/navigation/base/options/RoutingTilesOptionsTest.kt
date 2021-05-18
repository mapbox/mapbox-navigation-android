package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.internal.tilestore.TileStoreProvider
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.net.URISyntaxException
import kotlin.reflect.KClass

class RoutingTilesOptionsTest : BuilderTest<RoutingTilesOptions, RoutingTilesOptions.Builder>() {

    override fun getImplementationClass(): KClass<RoutingTilesOptions> =
        RoutingTilesOptions::class

    override fun getFilledUpBuilder(): RoutingTilesOptions.Builder {
        return RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com"))
            .tilesDataset("my_username.osm")
            .tilesProfile("driving")
            .tilesVersion("456")
            .filePath("123")
            .tileStore(TileStoreProvider.getTileStoreInstance("tile_store_path"))
            .minDaysBetweenServerAndLocalTilesVersion(0)
    }

    @Before
    fun setup() {
        mockkObject(TileStoreProvider)
        every { TileStoreProvider.getDefaultTileStoreInstance() } returns mockk()
        every { TileStoreProvider.getTileStoreInstance(any()) } returns mockk()
    }

    @After
    fun teardown() {
        unmockkObject(TileStoreProvider)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }

    @Test(expected = URISyntaxException::class)
    fun `tilesBaseUri should fail to build fake uri`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("fake uri"))
    }

    @Test
    fun `tilesBaseUri shouldn't fail if the URI has trailing slashes`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com/"))
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com///"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI does not have a scheme`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("my.api.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has path`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com/some/path"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has queries`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com/?arg=arg"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has fragments`() {
        RoutingTilesOptions.Builder()
            .tilesBaseUri(URI("https://my.api.com/#fragment"))
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot pass min days value less than zero`() {
        RoutingTilesOptions.Builder()
            .minDaysBetweenServerAndLocalTilesVersion(-1)
            .build()
    }

    @Test
    fun `default TileStore instance is used by default`() {
        val defaultOptions = RoutingTilesOptions.Builder().build()

        assertEquals(TileStoreProvider.getDefaultTileStoreInstance(), defaultOptions.tileStore)
    }

    @Test
    fun `custom TileStore instance overrides a default one`() {
        val customTileStore = TileStoreProvider.getTileStoreInstance("custom_tile_store_path")
        val options = RoutingTilesOptions.Builder()
            .tileStore(customTileStore)
            .build()

        assertEquals(customTileStore, options.tileStore)
    }
}

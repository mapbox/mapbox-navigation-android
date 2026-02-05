package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI
import java.net.URISyntaxException

@OptIn(ExperimentalMapboxNavigationAPI::class)
class DomainTilesOptionsTest {

    @Test(expected = URISyntaxException::class)
    fun `tilesBaseUri should fail to build fake uri`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("fake uri"))
    }

    @Test
    fun `tilesBaseUri shouldn't fail if the URI has trailing slashes`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("https://my.api.com/"))
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("https://my.api.com///"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI does not have a scheme`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("my.api.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has path`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("https://my.api.com/some/path"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has queries`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("https://my.api.com/?arg=arg"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tilesBaseUri should fail if the URI has fragments`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .tilesBaseUri(URI("https://my.api.com/#fragment"))
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot pass min days value less than zero`() {
        DomainTilesOptions.defaultHdTilesOptionsBuilder()
            .minDaysBetweenServerAndLocalTilesVersion(-1)
            .build()
    }

    @Test
    fun `default HD tiles config`() {
        val configFromBuilder = DomainTilesOptions.defaultHdTilesOptionsBuilder().build()
        val config = DomainTilesOptions.defaultHdTilesOptions()

        assertEquals(
            URI("https://api.mapbox.com"),
            configFromBuilder.tilesBaseUri,
        )
        assertEquals("mapbox", configFromBuilder.tilesDataset)
        assertEquals("", configFromBuilder.tilesProfile)
        assertEquals("", configFromBuilder.tilesVersion)
        assertEquals(7, configFromBuilder.minDaysBetweenServerAndLocalTilesVersion)

        assertEquals(configFromBuilder, config)
    }
}

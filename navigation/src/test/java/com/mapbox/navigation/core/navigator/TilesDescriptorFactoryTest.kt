package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.navigator.TilesetDescriptorFactory.NativeFactoryWrapper
import com.mapbox.navigator.CacheHandle
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TilesDescriptorFactoryTest {

    private val routingTilesOptions: RoutingTilesOptions = mockk(relaxed = true)
    private val cache: CacheHandle = mockk(relaxed = true)
    private val nativeFactoryWrapper: NativeFactoryWrapper = mockk(relaxed = true)
    private val tilesetDescriptorFactory by lazy {
        TilesetDescriptorFactory(routingTilesOptions, cache, nativeFactoryWrapper)
    }

    @Before
    fun setUp() {
        every { routingTilesOptions.tilesDataset } returns OPTIONS_DATASET
        every { routingTilesOptions.tilesProfile } returns OPTIONS_PROFILE
        every { routingTilesOptions.tilesVersion } returns OPTIONS_VERSION
    }

    @Test
    fun checkBuildWithParams() {
        tilesetDescriptorFactory.build(DATASET, PROFILE, VERSION, true)

        verify {
            nativeFactoryWrapper.build("$DATASET/$PROFILE", VERSION, true)
        }
    }

    @Test
    fun checkBuildWithDefaultDataset() {
        tilesetDescriptorFactory.build(
            tilesProfile = PROFILE,
            tilesVersion = VERSION,
            includeAdas = true,
        )

        verify {
            nativeFactoryWrapper.build("$OPTIONS_DATASET/$PROFILE", VERSION, true)
        }
    }

    @Test
    fun checkBuildWithDefaultProfile() {
        tilesetDescriptorFactory.build(
            tilesDataset = DATASET,
            tilesVersion = VERSION,
            includeAdas = true,
        )

        verify {
            nativeFactoryWrapper.build("$DATASET/$OPTIONS_PROFILE", VERSION, true)
        }
    }

    @Test
    fun checkBuildWithDefaultVersion() {
        tilesetDescriptorFactory.build(
            tilesDataset = DATASET,
            tilesProfile = PROFILE,
            includeAdas = true,
        )

        verify {
            nativeFactoryWrapper.build("$DATASET/$PROFILE", OPTIONS_VERSION, true)
        }
    }

    @Test
    fun checkBuildWithDefaultAdasis() {
        tilesetDescriptorFactory.build(
            tilesDataset = DATASET,
            tilesProfile = PROFILE,
            tilesVersion = VERSION,
        )

        verify {
            nativeFactoryWrapper.build("$DATASET/$PROFILE", VERSION)
        }
    }

    @Test
    fun checkBuildWithDefaults() {
        tilesetDescriptorFactory.build()

        verify {
            nativeFactoryWrapper.build("$OPTIONS_DATASET/$OPTIONS_PROFILE", OPTIONS_VERSION)
        }
    }

    private companion object {
        private const val DATASET = "dataset"
        private const val PROFILE = "profile"
        private const val VERSION = "version"

        private const val OPTIONS_DATASET = "options_dataset"
        private const val OPTIONS_PROFILE = "options_profile"
        private const val OPTIONS_VERSION = "options_version"
    }
}

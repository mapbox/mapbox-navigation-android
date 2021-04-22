package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.core.navigator.TilesetDescriptorFactory.NativeFactoryWrapper
import com.mapbox.navigator.CacheHandle
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

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
        tilesetDescriptorFactory.build(DATASET, PROFILE, VERSION)

        verify {
            nativeFactoryWrapper.build("$DATASET/$PROFILE", VERSION)
        }
    }

    @Test
    fun checkBuildWithDefaultDataset() {
        tilesetDescriptorFactory.build(tilesProfile = PROFILE, tilesVersion = VERSION)

        verify {
            nativeFactoryWrapper.build("$OPTIONS_DATASET/$PROFILE", VERSION)
        }
    }

    @Test
    fun checkBuildWithDefaultProfile() {
        tilesetDescriptorFactory.build(tilesDataset = DATASET, tilesVersion = VERSION)

        verify {
            nativeFactoryWrapper.build("$DATASET/$OPTIONS_PROFILE", VERSION)
        }
    }

    @Test
    fun checkBuildWithDefaultVersion() {
        tilesetDescriptorFactory.build(tilesDataset = DATASET, tilesProfile = PROFILE)

        verify {
            nativeFactoryWrapper.build("$DATASET/$PROFILE", OPTIONS_VERSION)
        }
    }

    @Test
    fun checkBuildWithDefaults() {
        tilesetDescriptorFactory.build()

        verify {
            nativeFactoryWrapper.build("$OPTIONS_DATASET/$OPTIONS_PROFILE", OPTIONS_VERSION)
        }
    }

    @Test
    fun checkBuildLatestLocalWithParams() {
        tilesetDescriptorFactory.buildLatestLocal(DATASET, PROFILE)

        verify {
            nativeFactoryWrapper.buildLatestLocal(cache, "$DATASET/$PROFILE")
        }
    }

    @Test
    fun checkBuildLatestLocalWithDefaultDataset() {
        tilesetDescriptorFactory.buildLatestLocal(tilesProfile = PROFILE)

        verify {
            nativeFactoryWrapper.buildLatestLocal(cache, "$OPTIONS_DATASET/$PROFILE")
        }
    }

    @Test
    fun checkBuildLatestLocalWithDefaultProfile() {
        tilesetDescriptorFactory.buildLatestLocal(tilesDataset = DATASET)

        verify {
            nativeFactoryWrapper.buildLatestLocal(cache, "$DATASET/$OPTIONS_PROFILE")
        }
    }

    @Test
    fun checkBuildLatestLocalWithDefaults() {
        tilesetDescriptorFactory.buildLatestLocal()

        verify {
            nativeFactoryWrapper.buildLatestLocal(cache, "$OPTIONS_DATASET/$OPTIONS_PROFILE")
        }
    }

    @Test
    fun checkBuildLatestServerWithParams() {
        tilesetDescriptorFactory.buildLatestServer(DATASET, PROFILE)

        verify {
            nativeFactoryWrapper.buildLatestServer(cache, "$DATASET/$PROFILE")
        }
    }

    @Test
    fun checkBuildLatestServerWithDefaultDataset() {
        tilesetDescriptorFactory.buildLatestServer(tilesProfile = PROFILE)

        verify {
            nativeFactoryWrapper.buildLatestServer(cache, "$OPTIONS_DATASET/$PROFILE")
        }
    }

    @Test
    fun checkBuildLatestServerWithDefaultProfile() {
        tilesetDescriptorFactory.buildLatestServer(tilesDataset = DATASET)

        verify {
            nativeFactoryWrapper.buildLatestServer(cache, "$DATASET/$OPTIONS_PROFILE")
        }
    }

    @Test
    fun checkBuildLatestServerWithDefaults() {
        tilesetDescriptorFactory.buildLatestServer()

        verify {
            nativeFactoryWrapper.buildLatestServer(cache, "$OPTIONS_DATASET/$OPTIONS_PROFILE")
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

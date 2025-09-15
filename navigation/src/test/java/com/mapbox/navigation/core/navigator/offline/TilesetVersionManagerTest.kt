package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.navigator.offline.TilesetVersionManager.AllTilesetsUpdatesCallback
import com.mapbox.navigation.testing.BlockingSamCallback
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TilesetVersionManagerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var tilesetVersionsApi: TilesetVersionsApi
    private lateinit var downloadedTilesetsFetcher: DownloadedTilesetsFetcher
    private lateinit var versionManager: TilesetVersionManager

    @Before
    fun setUp() {
        tilesetVersionsApi = mockk(relaxed = true)
        downloadedTilesetsFetcher = mockk(relaxed = true)

        versionManager = TilesetVersionManagerImpl(
            tilesBaseUri = TILES_BASE_URI,
            tilesDataset = TILES_DATASET,
            tilesProfile = TILES_PROFILE,
            downloadedTilesetsFetcher = downloadedTilesetsFetcher,
            tilesetVersionsApi = tilesetVersionsApi,
        )
    }

    @Test
    fun `getAvailableVersions callback should return success with versions`() {
        val response = TilesetVersionsApi.RouteTileVersionsResponse(
            availableVersions = listOf(
                "2025_07_25-12_01_23",
                "2025_07_24-11_30_15",
                "2025_07_23-10_45_30",
            ),
            blockedVersions = setOf("2025_07_23-10_45_30"),
        )

        tilesetVersionsApi.mockResponse(response)

        val result = versionManager.getAvailableVersionsBlocking()

        assertTrue(result.isValue)

        val versions = result.value!!
        assertEquals(3, versions.size)

        val latestVersion = versions[0]
        assertEquals("2025_07_25-12_01_23", latestVersion.versionName)
        assertTrue(latestVersion.isLatest)
        assertFalse(latestVersion.isBlocked)
        verifyVersionReleaseDate(latestVersion, "2025_07_25-12_01_23")

        val secondVersion = versions[1]
        assertEquals("2025_07_24-11_30_15", secondVersion.versionName)
        assertFalse(secondVersion.isLatest)
        assertFalse(secondVersion.isBlocked)
        verifyVersionReleaseDate(secondVersion, "2025_07_24-11_30_15")

        val thirdVersion = versions[2]
        assertEquals("2025_07_23-10_45_30", thirdVersion.versionName)
        assertFalse(thirdVersion.isLatest)
        assertTrue(thirdVersion.isBlocked)
        verifyVersionReleaseDate(thirdVersion, "2025_07_23-10_45_30")
    }

    @Test
    fun `getAvailableVersions callback should handle empty versions list`() {
        val response = TilesetVersionsApi.RouteTileVersionsResponse(
            availableVersions = emptyList(),
            blockedVersions = emptySet(),
        )

        tilesetVersionsApi.mockResponse(response)

        val result = versionManager.getAvailableVersionsBlocking()
        assertTrue(result.isValue)
        assertTrue(result.value!!.isEmpty())
    }

    @Test
    fun `getAvailableVersions callback should handle API error`() {
        val error = RuntimeException("API Error")
        tilesetVersionsApi.mockError(error)

        val result = versionManager.getAvailableVersionsBlocking()

        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `getAvailableVersions suspend function should work correctly`() = runBlocking {
        val response = TilesetVersionsApi.RouteTileVersionsResponse(
            availableVersions = listOf("2025_07_25-12_01_23", "2025_07_24-11_30_15"),
            blockedVersions = emptySet(),
        )

        tilesetVersionsApi.mockResponse(response)

        val result = versionManager.getAvailableVersions()

        assertTrue(result.isValue)
        val versions = result.value!!
        assertEquals(2, versions.size)
    }

    @Test
    fun `getAvailableVersions should call API with correct parameters`() {
        val response = TilesetVersionsApi.RouteTileVersionsResponse(
            availableVersions = emptyList(),
            blockedVersions = emptySet(),
        )

        tilesetVersionsApi.mockResponse(response)

        versionManager.getAvailableVersionsBlocking()

        verify(exactly = 1) {
            tilesetVersionsApi.getRouteTileVersions(
                baseUri = TILES_BASE_URI,
                dataset = TILES_DATASET,
                profile = TILES_PROFILE,
                callback = any(),
            )
        }
    }

    @Test
    fun `getAvailableVersions should return cancellable`() {
        val apiCancelable = mockk<Cancelable>(relaxed = true)
        every {
            tilesetVersionsApi.getRouteTileVersions(any(), any(), any(), any())
        } returns apiCancelable

        val result = versionManager.getAvailableVersions(mockk(relaxed = true))
        result.cancel()

        verify(exactly = 1) {
            apiCancelable.cancel()
        }
    }

    @Test
    fun `getAvailableUpdate callback should return no updates when no downloaded tilesets`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(emptyList())

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.NoUpdates)
    }

    @Test
    fun `getAvailableUpdate callback should return no updates when versions are up to date`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_25-12_01_23",
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.NoUpdates)
    }

    @Test
    fun `getAvailableUpdate callback should return update available when version is older than threshold`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_24-12_01_23", // 1 day older
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.Available)

        val availableResult = result.value as TilesetUpdateAvailabilityResult.Available
        assertEquals(regionId, availableResult.regionId)
        assertEquals("2025_07_24-12_01_23", availableResult.currentVersion)
        assertEquals("2025_07_25-12_01_23", availableResult.latestVersion)
        assertFalse(availableResult.isAsap)
    }

    @Test
    fun `getAvailableUpdate callback should return ASAP update when version is blocked`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_23-12_01_23", // Blocked version
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23", "2025_07_23-12_01_23"),
                blockedVersions = setOf("2025_07_23-12_01_23"),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.Available)

        val availableResult = result.value as TilesetUpdateAvailabilityResult.Available
        assertEquals(regionId, availableResult.regionId)
        assertEquals("2025_07_23-12_01_23", availableResult.currentVersion)
        assertEquals("2025_07_25-12_01_23", availableResult.latestVersion)
        assertTrue(availableResult.isAsap)
    }

    @Test
    fun `getAvailableUpdate callback should handle multiple navigation tilesets by selecting latest`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_24-12_01_23",
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_25-12_01_23", // Latest
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.NoUpdates)
    }

    @Test
    fun `getAvailableUpdate callback should fallback to ADAS tilesets when no navigation tilesets`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Adas",
                version = "2025_07_24-12_01_23",
                dataset = TILES_DATASET,
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.Available)

        val availableResult = result.value as TilesetUpdateAvailabilityResult.Available
        assertEquals("2025_07_24-12_01_23", availableResult.currentVersion)
        assertEquals("2025_07_25-12_01_23", availableResult.latestVersion)
    }

    @Test
    fun `getAvailableUpdate callback should handle API error`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L
        val error = RuntimeException("API Error")

        tilesetVersionsApi.mockError(error)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `getAvailableUpdate callback should handle metadata fetch error`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L
        val error = RuntimeException("Metadata Error")

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockError(error)

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `getAvailableUpdate callback should handle empty available versions`() {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = emptyList(),
                blockedVersions = emptySet(),
            ),
        )

        val result = versionManager.getAvailableUpdateBlocking(
            regionId,
            maxAllowedAgeDifferenceMinutes,
        )

        assertTrue(result.isError)
        assertTrue(result.error!!.message!!.contains("No available versions received"))
    }

    @Test
    fun `getAvailableUpdate suspend function should work correctly`() = runBlocking {
        val regionId = "test-region"
        val maxAllowedAgeDifferenceMinutes = 60L

        val downloadedTilesets = listOf(
            DownloadedTileset(
                domain = "Navigation",
                version = "2025_07_24-12_01_23",
                dataset = "$TILES_DATASET/$TILES_PROFILE",
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockResponse(downloadedTilesets)

        val result = versionManager.getAvailableUpdate(regionId, maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        assertTrue(result.value is TilesetUpdateAvailabilityResult.Available)
    }

    @Test
    fun `getAvailableUpdates callback should return success with updates for multiple regions`() {
        val maxAllowedAgeDifferenceMinutes = 60L

        val availableVersions = listOf("2025_07_25-12_01_23", "2025_07_24-11_30_15")
        val regionsMetadata = listOf(
            "region1" to listOf(
                DownloadedTileset(
                    domain = "Navigation",
                    version = "2025_07_24-12_01_23", // Older version
                    dataset = "$TILES_DATASET/$TILES_PROFILE",
                ),
            ),
            "region2" to listOf(
                DownloadedTileset(
                    domain = "Navigation",
                    version = "2025_07_25-12_01_23", // Latest version
                    dataset = "$TILES_DATASET/$TILES_PROFILE",
                ),
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = availableVersions,
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsResponse(regionsMetadata)

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        val updates = result.value!!
        assertEquals(1, updates.size) // Only region1 should have updates

        val availableUpdate = updates[0]
        assertEquals("region1", availableUpdate.regionId)
        assertEquals("2025_07_24-12_01_23", availableUpdate.currentVersion)
        assertEquals("2025_07_25-12_01_23", availableUpdate.latestVersion)
    }

    @Test
    fun `getAvailableUpdates callback should return empty list when no updates needed`() {
        val maxAllowedAgeDifferenceMinutes = 60L

        val availableVersions = listOf("2025_07_25-12_01_23")
        val regionsMetadata = listOf(
            "region1" to listOf(
                DownloadedTileset(
                    domain = "Navigation",
                    version = "2025_07_25-12_01_23", // Latest version
                    dataset = "$TILES_DATASET/$TILES_PROFILE",
                ),
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = availableVersions,
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsResponse(regionsMetadata)

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        assertTrue(result.value!!.isEmpty())
    }

    @Test
    fun `getAvailableUpdates callback should return updates for blocked versions`() {
        val maxAllowedAgeDifferenceMinutes = 60L

        val availableVersions = listOf("2025_07_25-12_01_23", "2025_07_23-10_45_30")
        val regionsMetadata = listOf(
            "region1" to listOf(
                DownloadedTileset(
                    domain = "Navigation",
                    version = "2025_07_23-10_45_30", // Blocked version
                    dataset = "$TILES_DATASET/$TILES_PROFILE",
                ),
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = availableVersions,
                blockedVersions = setOf("2025_07_23-10_45_30"),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsResponse(regionsMetadata)

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        val updates = result.value!!
        assertEquals(1, updates.size)

        assertTrue(updates[0].isAsap) // Should be ASAP due to blocked version
    }

    @Test
    fun `getAvailableUpdates callback should handle empty regions list`() {
        val maxAllowedAgeDifferenceMinutes = 60L

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsResponse(emptyList())

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        assertTrue(result.value!!.isEmpty())
    }

    @Test
    fun `getAvailableUpdates callback should handle API error`() {
        val maxAllowedAgeDifferenceMinutes = 60L
        val error = RuntimeException("API Error")

        tilesetVersionsApi.mockError(error)

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `getAvailableUpdates callback should handle metadata fetch error`() {
        val maxAllowedAgeDifferenceMinutes = 60L
        val error = RuntimeException("Metadata Error")

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsError(error)

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isError)
        assertEquals("Unable to get tile regions: Metadata Error", result.error?.message)
    }

    @Test
    fun `getAvailableUpdates callback should handle empty available versions`() {
        val maxAllowedAgeDifferenceMinutes = 60L

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = emptyList(),
                blockedVersions = emptySet(),
            ),
        )

        val result = versionManager.getAvailableUpdatesBlocking(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        assertTrue(result.value?.isEmpty() == true)
    }

    @Test
    fun `getAvailableUpdates callback should return cancellable`() {
        val apiCancelable = mockk<Cancelable>(relaxed = true)
        every {
            tilesetVersionsApi.getRouteTileVersions(any(), any(), any(), any())
        } returns apiCancelable

        val result = versionManager.getAvailableUpdates(
            mockk<AllTilesetsUpdatesCallback>(relaxed = true),
        )
        result.cancel()

        verify(exactly = 1) {
            apiCancelable.cancel()
        }
    }

    @Test
    fun `getAvailableUpdates suspend function should work correctly`() = runBlocking {
        val maxAllowedAgeDifferenceMinutes = 60L

        val regionsMetadata = listOf(
            "region1" to listOf(
                DownloadedTileset(
                    domain = "Navigation",
                    version = "2025_07_24-12_01_23",
                    dataset = "$TILES_DATASET/$TILES_PROFILE",
                ),
            ),
        )

        tilesetVersionsApi.mockResponse(
            TilesetVersionsApi.RouteTileVersionsResponse(
                availableVersions = listOf("2025_07_25-12_01_23"),
                blockedVersions = emptySet(),
            ),
        )

        downloadedTilesetsFetcher.mockAllRegionsResponse(regionsMetadata)

        val result = versionManager.getAvailableUpdates(maxAllowedAgeDifferenceMinutes)

        assertTrue(result.isValue)
        val updates = result.value!!
        assertEquals(1, updates.size)
    }

    private companion object {

        const val TILES_BASE_URI = "https://api.mapbox.com"
        const val TILES_DATASET = "test-dataset"
        const val TILES_PROFILE = "driving"

        private val VERSION_DATE_FORMATTER =
            SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        fun TilesetVersionManager.getAvailableVersionsBlocking():
            Expected<Throwable, List<TilesetVersion>> {
            val callback = BlockingSamCallback<Expected<Throwable, List<TilesetVersion>>>()
            getAvailableVersions(callback)
            return callback.getResultBlocking()
        }

        fun verifyVersionReleaseDate(version: TilesetVersion, expectedVersionName: String) {
            val formattedDate = VERSION_DATE_FORMATTER.format(version.releaseDate!!)
            assertEquals(
                "Version release date should match version name",
                expectedVersionName,
                formattedDate,
            )
        }

        fun TilesetVersionsApi.mockResponse(
            response: TilesetVersionsApi.RouteTileVersionsResponse,
        ): CapturingSlot<(ExpectedRouteTileVersionsCallback) -> Unit> {
            val callbackSlot = slot<(ExpectedRouteTileVersionsCallback) -> Unit>()
            every {
                getRouteTileVersions(any(), any(), any(), capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createValue(response))
                mockk<Cancelable>()
            }
            return callbackSlot
        }

        fun TilesetVersionsApi.mockError(
            error: Throwable,
        ): CapturingSlot<(ExpectedRouteTileVersionsCallback) -> Unit> {
            val callbackSlot =
                slot<(ExpectedRouteTileVersionsCallback) -> Unit>()
            every {
                getRouteTileVersions(any(), any(), any(), capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createError(error))
                mockk<Cancelable>()
            }
            return callbackSlot
        }

        fun DownloadedTilesetsFetcher.mockResponse(
            response: List<DownloadedTileset>,
        ): CapturingSlot<(TilesetDescriptorsMetadata) -> Unit> {
            val callbackSlot = slot<(TilesetDescriptorsMetadata) -> Unit>()
            every {
                getTilesetDescriptorsMetadata(any(), capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createValue(response))
                mockk<Cancelable>()
            }
            return callbackSlot
        }

        fun DownloadedTilesetsFetcher.mockError(
            error: Throwable,
        ): CapturingSlot<(TilesetDescriptorsMetadata) -> Unit> {
            val callbackSlot = slot<(TilesetDescriptorsMetadata) -> Unit>()
            every {
                getTilesetDescriptorsMetadata(any(), capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createError(error))
                mockk<Cancelable>()
            }
            return callbackSlot
        }

        fun TilesetVersionManager.getAvailableUpdateBlocking(
            regionId: String,
            maxAllowedAgeDifferenceMinutes: Long,
        ): Expected<Throwable, TilesetUpdateAvailabilityResult> {
            val callback = BlockingSamCallback<
                Expected<Throwable, TilesetUpdateAvailabilityResult>,
                >()
            getAvailableUpdate(regionId, maxAllowedAgeDifferenceMinutes, callback)
            return callback.getResultBlocking()
        }

        fun TilesetVersionManager.getAvailableUpdatesBlocking(
            maxAllowedAgeDifferenceMinutes: Long,
        ): Expected<Throwable, List<TilesetUpdateAvailabilityResult.Available>> {
            val callback = BlockingSamCallback<
                Expected<Throwable, List<TilesetUpdateAvailabilityResult.Available>>,
                >()
            getAvailableUpdates(maxAllowedAgeDifferenceMinutes, callback)
            return callback.getResultBlocking()
        }

        fun DownloadedTilesetsFetcher.mockAllRegionsResponse(
            response: List<Pair<String, List<DownloadedTileset>>>,
        ): CapturingSlot<(AllTilesetsDescriptorMetadataResult) -> Unit> {
            val callbackSlot = slot<(AllTilesetsDescriptorMetadataResult) -> Unit>()
            every {
                allTilesetsDescriptorMetadata(capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createValue(response))
                mockk<Cancelable>()
            }
            return callbackSlot
        }

        fun DownloadedTilesetsFetcher.mockAllRegionsError(
            error: Throwable,
        ): CapturingSlot<(AllTilesetsDescriptorMetadataResult) -> Unit> {
            val callbackSlot = slot<(AllTilesetsDescriptorMetadataResult) -> Unit>()
            every {
                allTilesetsDescriptorMetadata(capture(callbackSlot))
            } answers {
                callbackSlot.captured(ExpectedFactory.createError(error))
                mockk<Cancelable>()
            }
            return callbackSlot
        }
    }
}

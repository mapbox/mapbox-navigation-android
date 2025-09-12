package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.Cancelable
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.navigator.offline.TilesetVersionsApi.RouteTileVersionsResponse
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class TilesetVersionManagerImpl internal constructor(
    private val tilesBaseUri: String,
    private val tilesDataset: String,
    private val tilesProfile: String,
    private val downloadedTilesetsFetcher: DownloadedTilesetsFetcher,
    private val tilesetVersionsApi: TilesetVersionsApi = TilesetVersionsApi(),
) : TilesetVersionManager {

    override fun getAvailableVersions(
        callback: TilesetVersionManager.TilesetVersionsCallback,
    ): Cancelable {
        return tilesetVersionsApi.getRouteTileVersions(
            baseUri = tilesBaseUri,
            dataset = tilesDataset,
            profile = tilesProfile,
        ) { response ->
            callback.onVersionsResult(response.mapValue { it.toTilesetVersions() })
        }
    }

    override fun getAvailableUpdates(
        maxAllowedAgeDifferenceMinutes: Long,
        callback: TilesetVersionManager.AllTilesetsUpdatesCallback,
    ): Cancelable {
        return getAvailableVersions { versionsResponse ->
            versionsResponse.onError { error ->
                callback.onUpdatesResult(ExpectedFactory.createError(error))
            }.onValue { versions ->
                getAvailableUpdates(maxAllowedAgeDifferenceMinutes, versions, callback)
            }
        }
    }

    override fun getAvailableUpdate(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long,
        callback: TilesetVersionManager.TilesetUpdatesCallback,
    ): Cancelable {
        return getAvailableVersions { versionsResponse ->
            versionsResponse.onError { error ->
                callback.onUpdatesResult(ExpectedFactory.createError(error))
            }.onValue { versions ->
                if (versions.isEmpty()) {
                    callback.onUpdatesResult(
                        ExpectedFactory.createError(
                            RuntimeException("No available versions received"),
                        ),
                    )
                } else {
                    isUpdateAvailable(regionId, maxAllowedAgeDifferenceMinutes, versions, callback)
                }
            }
        }
    }

    private fun getAvailableUpdates(
        maxAllowedAgeDifferenceMinutes: Long,
        versions: List<TilesetVersion>,
        callback: TilesetVersionManager.AllTilesetsUpdatesCallback,
    ) {
        if (versions.isEmpty()) {
            callback.onUpdatesResult(
                ExpectedFactory.createValue(emptyList()),
            )
            return
        }

        downloadedTilesetsFetcher.allTilesetsDescriptorMetadata { response ->
            response.onError {
                callback.onUpdatesResult(
                    ExpectedFactory.createError(
                        RuntimeException("Unable to get tile regions: ${it.message ?: it}"),
                    ),
                )
            }.onValue { value ->
                if (value.isEmpty()) {
                    callback.onUpdatesResult(
                        ExpectedFactory.createValue(emptyList()),
                    )
                    return@onValue
                }
                val mapped = value.map { (regionId, downloadedTilesets) ->
                    isUpdateAvailable(
                        regionId,
                        maxAllowedAgeDifferenceMinutes,
                        versions,
                        downloadedTilesets,
                    )
                }

                val updates = mapped.mapNotNull {
                    it.value as? TilesetUpdateAvailabilityResult.Available
                }

                val errors = mapped.mapNotNull { it.error }

                if (updates.isNotEmpty()) {
                    if (errors.isNotEmpty()) {
                        logD(LOG_CATEGORY) {
                            "Errors occurred during updates processing: " +
                                errors.joinToString { it.message ?: "<no error message>" }
                        }
                    }
                    callback.onUpdatesResult(ExpectedFactory.createValue(updates))
                } else if (errors.isEmpty()) {
                    callback.onUpdatesResult(ExpectedFactory.createValue(emptyList()))
                } else {
                    callback.onUpdatesResult(
                        ExpectedFactory.createError(
                            RuntimeException(
                                "Error occurred during updates processing: " +
                                    errors.joinToString { it.message ?: "<no error message>" },
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun isUpdateAvailable(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long,
        availableVersions: List<TilesetVersion>,
        callback: TilesetVersionManager.TilesetUpdatesCallback,
    ) {
        downloadedTilesetsFetcher.getTilesetDescriptorsMetadata(regionId) { metadataResponse ->
            metadataResponse.onError { error ->
                callback.onUpdatesResult(ExpectedFactory.createError(error))
            }.onValue { metadata ->
                callback.onUpdatesResult(
                    isUpdateAvailable(
                        regionId,
                        maxAllowedAgeDifferenceMinutes,
                        availableVersions,
                        metadata,
                    ),
                )
            }
        }
    }

    private fun isUpdateAvailable(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long,
        availableVersions: List<TilesetVersion>,
        metadata: List<DownloadedTileset>,
    ): Expected<Throwable, TilesetUpdateAvailabilityResult> {
        logD(LOG_CATEGORY) {
            "isUpdateAvailable(" +
                "$regionId, " +
                "$maxAllowedAgeDifferenceMinutes, " +
                "$availableVersions, " +
                "$availableVersions" +
                ")"
        }

        val navTilesets = metadata.filter {
            it.dataset == "$tilesDataset/$tilesProfile" && it.isNavigationDomain
        }.toSet()

        logD(LOG_CATEGORY) {
            "Matching Navigation tilesets: $navTilesets"
        }

        val latestDownloadedVersion = when {
            navTilesets.size > 1 -> {
                // Error happened, there shouldn't be more than one Nav tileset
                // of different versions
                logD(LOG_CATEGORY) {
                    "More than one navigation tileset available: $navTilesets"
                }
                navTilesets.maxBy { it.version ?: "" }
            }
            navTilesets.size == 1 -> {
                navTilesets.first()
            }
            else -> {
                val adasTilesets = metadata.filter {
                    it.dataset == tilesDataset && it.isAdasDomain
                }.toSet()

                if (adasTilesets.isNotEmpty()) {
                    // Error happened, no ADAS tilesets should exist without Nav tilesets
                    logD(LOG_CATEGORY) {
                        "No navigation tilesets found, but ADAS tilesets available: $adasTilesets"
                    }
                    adasTilesets.maxBy { it.version ?: "" }
                } else {
                    null
                }
            }
        }

        val latestVersion = availableVersions.first { it.isLatest }

        logD(LOG_CATEGORY) {
            "Latest available version: $latestVersion, latest downloaded: $latestDownloadedVersion"
        }

        if (latestDownloadedVersion == null) {
            return ExpectedFactory.createValue(TilesetUpdateAvailabilityResult.NoUpdates)
        }

        val isBlocked = availableVersions.any {
            it.versionName == latestDownloadedVersion.version && it.isBlocked
        }

        val result = if (isBlocked) {
            TilesetUpdateAvailabilityResult.Available(
                regionId = regionId,
                isAsap = true,
                currentVersion = latestDownloadedVersion.version ?: "",
                latestVersion = latestVersion.versionName,
            )
        } else {
            val latestAvailableVersionDate = latestVersion.releaseDate
            val downloadedVersionDate = latestDownloadedVersion.releaseDate

            if (latestAvailableVersionDate == null || downloadedVersionDate == null) {
                logW(LOG_CATEGORY) {
                    "Can't compare null dates. Date1: $latestAvailableVersionDate, " +
                        "Date2: $downloadedVersionDate"
                }

                TilesetUpdateAvailabilityResult.Available(
                    regionId = regionId,
                    isAsap = false,
                    currentVersion = latestDownloadedVersion.version ?: "",
                    latestVersion = latestVersion.versionName,
                )
            } else {
                val diffMillis = latestAvailableVersionDate.time - downloadedVersionDate.time
                val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)

                logD(LOG_CATEGORY) {
                    "Diff between available and downloaded versions: $diffMinutes minutes"
                }

                if (diffMinutes > maxAllowedAgeDifferenceMinutes) {
                    TilesetUpdateAvailabilityResult.Available(
                        regionId = regionId,
                        isAsap = false,
                        currentVersion = latestDownloadedVersion.version ?: "",
                        latestVersion = latestVersion.versionName,
                    )
                } else {
                    TilesetUpdateAvailabilityResult.NoUpdates
                }
            }
        }
        return ExpectedFactory.createValue(result)
    }

    override suspend fun getAvailableVersions(): Expected<Throwable, List<TilesetVersion>> {
        return suspendCancellableCoroutine { continuation ->
            val cancelable = getAvailableVersions { result ->
                continuation.resume(result)
            }

            continuation.invokeOnCancellation {
                cancelable.cancel()
            }
        }
    }

    override suspend fun getAvailableUpdates(
        maxAllowedAgeDifferenceMinutes: Long,
    ): Expected<Throwable, List<TilesetUpdateAvailabilityResult.Available>> {
        return suspendCancellableCoroutine { continuation ->
            val cancelable = getAvailableUpdates(maxAllowedAgeDifferenceMinutes) {
                continuation.resume(it)
            }

            continuation.invokeOnCancellation {
                cancelable.cancel()
            }
        }
    }

    override suspend fun getAvailableUpdate(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long,
    ): Expected<Throwable, TilesetUpdateAvailabilityResult> {
        return suspendCancellableCoroutine { continuation ->
            val cancelable = getAvailableUpdate(regionId, maxAllowedAgeDifferenceMinutes) {
                continuation.resume(it)
            }

            continuation.invokeOnCancellation {
                cancelable.cancel()
            }
        }
    }

    companion object {

        @JvmSynthetic
        internal val LOG_CATEGORY = "TilesetVersionManager"

        private fun RouteTileVersionsResponse.toTilesetVersions(): List<TilesetVersion> {
            // Versions look like the following
            // ["2025_07_25-12_01_23", ..., "2024_02_23-12_00_19"]
            // So by sorting them lexicographically, max version is the latest one
            val latestVersion = availableVersions.maxOrNull()

            return availableVersions.map { version ->
                TilesetVersion(
                    versionName = version,
                    releaseDate = TilesetReleaseDateParser.parseReleaseDate(version),
                    isLatest = version == latestVersion,
                    isBlocked = blockedVersions.contains(version),
                )
            }
        }
    }
}

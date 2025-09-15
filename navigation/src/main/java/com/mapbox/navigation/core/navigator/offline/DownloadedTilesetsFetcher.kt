package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.TileStore
import com.mapbox.navigation.utils.internal.logD
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Serializable
internal data class DownloadedTileset(
    val domain: String? = null,
    val version: String? = null,
    val dataset: String? = null,
) {

    val isAdasDomain: Boolean
        get() = "adas".equals(domain, ignoreCase = true)

    val isNavigationDomain: Boolean
        get() = "navigation".equals(domain, ignoreCase = true)

    val releaseDate: Date? by lazy {
        version?.let {
            TilesetReleaseDateParser.parseReleaseDate(it)
        }
    }
}

internal typealias AllTilesetsDescriptorMetadataResult =
    Expected<Throwable, List<Pair<String, List<DownloadedTileset>>>>

internal typealias TilesetDescriptorsMetadata = Expected<Throwable, List<DownloadedTileset>>

internal class DownloadedTilesetsFetcher(
    private val tileStore: TileStore,
) {

    fun allTilesetsDescriptorMetadata(
        callback: (AllTilesetsDescriptorMetadataResult) -> Unit,
    ) {
        getAllTileRegionIds { response ->
            response.onError {
                callback(
                    ExpectedFactory.createError(
                        RuntimeException("Unable to get descriptors metadata: $it"),
                    ),
                )
            }.onValue { regions ->
                if (regions.isEmpty()) {
                    callback(ExpectedFactory.createValue(emptyList()))
                    return@onValue
                }

                val results = CopyOnWriteArrayList<Pair<String, List<DownloadedTileset>>>()
                val errors = CopyOnWriteArrayList<Throwable>()
                val completedCount = AtomicInteger(0)

                fun checkResultsAndCallback() {
                    if (completedCount.incrementAndGet() != regions.size) {
                        return
                    }

                    if (results.isNotEmpty()) {
                        if (errors.isNotEmpty()) {
                            logD(TilesetVersionManagerImpl.LOG_CATEGORY) {
                                "Errors occurred during descriptors processing: " +
                                    errors.joinToString { it.message ?: "<no error message>" }
                            }
                        }
                        callback(ExpectedFactory.createValue(results))
                    } else {
                        callback(
                            ExpectedFactory.createError(
                                RuntimeException(
                                    "Errors occurred during descriptors processing: " +
                                        errors.joinToString { it.message ?: "<no error message>" },
                                ),
                            ),
                        )
                    }
                }

                regions.forEach { regionId ->
                    getTilesetDescriptorsMetadata(regionId) { metadataResponse ->
                        metadataResponse.onError { error ->
                            errors.add(error)
                            checkResultsAndCallback()
                        }.onValue { metadata ->
                            results.add(regionId to metadata)
                            checkResultsAndCallback()
                        }
                    }
                }
            }
        }
    }

    fun getTilesetDescriptorsMetadata(
        regionId: String,
        callback: (TilesetDescriptorsMetadata) -> Unit,
    ) {
        tileStore.getTileRegionTilesets(regionId) {
            it.onError { tileRegionError ->
                callback(
                    ExpectedFactory.createError(
                        RuntimeException("Unable to get tile region: $tileRegionError"),
                    ),
                )
            }.onValue { descriptor ->
                descriptor.toValue { descriptorToValueResult ->
                    descriptorToValueResult.onError { error ->
                        callback(
                            ExpectedFactory.createError(
                                RuntimeException("Unable to get descriptor metadata: $error"),
                            ),
                        )
                    }.onValue { value ->
                        val metadata = parseFromJson(value.toJson())
                        callback(ExpectedFactory.createValue(metadata))
                    }
                }
            }
        }
    }

    private fun getAllTileRegionIds(callback: (Expected<Throwable, List<String>>) -> Unit) {
        tileStore.getAllTileRegions { response ->
            response.onError {
                callback.invoke(
                    ExpectedFactory.createError(
                        RuntimeException("Unable to get tile region: $it"),
                    ),
                )
            }.onValue { value ->
                callback(ExpectedFactory.createValue(value.map { it.id }))
            }
        }
    }

    @Serializable
    private data class TilesetDescriptorMetadataResponse(
        val resolved: List<DownloadedTileset>,
    )

    companion object {

        private val jsonParser = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        fun parseFromJson(json: String): List<DownloadedTileset> {
            return try {
                val response = jsonParser.decodeFromString<TilesetDescriptorMetadataResponse>(json)
                response.resolved
            } catch (e: kotlinx.serialization.SerializationException) {
                logD(TilesetVersionManagerImpl.LOG_CATEGORY) {
                    "Downloaded tilesets metadata parsing error: $e"
                }
                emptyList()
            } catch (e: Exception) {
                logD(TilesetVersionManagerImpl.LOG_CATEGORY) {
                    "Downloaded tilesets metadata error: $e"
                }
                emptyList()
            }
        }
    }
}

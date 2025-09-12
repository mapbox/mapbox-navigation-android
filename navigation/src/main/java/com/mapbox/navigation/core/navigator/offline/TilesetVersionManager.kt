package com.mapbox.navigation.core.navigator.offline

import com.mapbox.bindgen.Expected
import com.mapbox.common.Cancelable
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Manager for checking offline tileset version availability and update requirements.
 * This interface provides functionality to check for available tileset versions and determine
 * if downloaded tilesets need updates based on version comparison and age thresholds.
 * @see TilesetVersion for information about tileset version metadata
 * @see TilesetUpdateAvailabilityResult for update availability results
 */
@ExperimentalPreviewMapboxNavigationAPI
interface TilesetVersionManager {

    /**
     * Retrieves the list of available tileset versions from the server.
     * @param callback Callback to receive the result containing available versions
     * @return Cancelable operation that can be cancelled
     */
    fun getAvailableVersions(callback: TilesetVersionsCallback): Cancelable

    /**
     * Checks for available tileset updates across all downloaded regions.
     * This is a convenience method that uses a default age threshold of 0 minutes,
     * meaning any version difference will trigger an update recommendation.
     * @param callback Callback to receive the list of available updates
     * @return Cancelable operation that can be cancelled
     */
    fun getAvailableUpdates(
        callback: AllTilesetsUpdatesCallback,
    ): Cancelable = getAvailableUpdates(0, callback)

    /**
     * Checks for available tileset updates across all downloaded regions based on age threshold.
     * This method checks all downloaded regions and determines which ones have updates available
     * based on version comparison and age thresholds. It compares the currently downloaded
     * tileset versions with the latest available versions and determines if updates are needed.
     * @param maxAllowedAgeDifferenceMinutes Maximum age difference in minutes before update is recommended
     * @param callback Callback to receive the list of available updates
     * @return Cancelable operation that can be cancelled
     */
    fun getAvailableUpdates(
        maxAllowedAgeDifferenceMinutes: Long,
        callback: AllTilesetsUpdatesCallback,
    ): Cancelable

    /**
     * Checks if a tileset update is available for the specified region.
     * This is a convenience method that uses a default age threshold of 0 minutes,
     * meaning any version difference will trigger an update recommendation.
     * @param regionId The ID of the region to check for updates
     * @param callback Callback to receive the update availability result
     * @return Cancelable operation that can be cancelled
     */
    fun getAvailableUpdate(
        regionId: String,
        callback: TilesetUpdatesCallback,
    ): Cancelable = getAvailableUpdate(regionId, 0, callback)

    /**
     * Checks if a tileset update is available for the specified region based on age threshold.
     * The method compares the currently downloaded tileset version with the latest available
     * version and determines if an update is needed based on:
     * - Version differences (if versions don't match)
     * - Age threshold (if current version is older than the specified threshold)
     * - Blocked versions (if current version is marked as blocked)
     *
     * @param regionId The ID of the region to check for updates
     * @param maxAllowedAgeDifferenceMinutes Maximum age difference in minutes before update is recommended
     * @param callback Callback to receive the update availability result
     * @return Cancelable operation that can be cancelled
     */
    fun getAvailableUpdate(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long,
        callback: TilesetUpdatesCallback,
    ): Cancelable

    /**
     * Retrieves the list of available tileset versions from the server.
     * @return Expected containing either the list of available versions or an error
     */
    suspend fun getAvailableVersions(): Expected<Throwable, List<TilesetVersion>>

    /**
     * Checks for available tileset updates across all downloaded regions based on age threshold.
     * @param maxAllowedAgeDifferenceMinutes Maximum age difference in minutes before update is recommended
     * @return Expected containing either the list of available updates or an error
     */
    suspend fun getAvailableUpdates(
        maxAllowedAgeDifferenceMinutes: Long = 0,
    ): Expected<Throwable, List<TilesetUpdateAvailabilityResult.Available>>

    /**
     * Checks if a tileset update is available for the specified region based on age threshold.
     * @param regionId The ID of the region to check for updates
     * @param maxAllowedAgeDifferenceMinutes Maximum age difference in minutes before update is recommended
     * @return Expected containing either the update availability result or an error
     */
    suspend fun getAvailableUpdate(
        regionId: String,
        maxAllowedAgeDifferenceMinutes: Long = 0,
    ): Expected<Throwable, TilesetUpdateAvailabilityResult>

    /**
     * Callback interface for receiving available tileset updates across all regions.
     */
    fun interface AllTilesetsUpdatesCallback {
        /**
         * Called when the updates availability check completes for all regions.
         * @param result Expected containing either the list of available updates or an error
         */
        fun onUpdatesResult(
            result: Expected<Throwable, List<TilesetUpdateAvailabilityResult.Available>>,
        )
    }

    /**
     * Callback interface for receiving tileset update availability results for a single region.
     */
    fun interface TilesetUpdatesCallback {
        /**
         * Called when the update availability check completes for a specific region.
         * @param result Expected containing either the update availability result or an error
         */
        fun onUpdatesResult(result: Expected<Throwable, TilesetUpdateAvailabilityResult>)
    }

    /**
     * Callback interface for receiving available tileset versions.
     */
    fun interface TilesetVersionsCallback {
        /**
         * Called when the available versions request completes.
         * @param versions Expected containing either the list of available versions or an error
         */
        fun onVersionsResult(versions: Expected<Throwable, List<TilesetVersion>>)
    }
}

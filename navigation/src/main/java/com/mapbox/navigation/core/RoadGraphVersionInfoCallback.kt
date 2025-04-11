package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Interface definition for a callback to be invoked when road graph version requested.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface RoadGraphVersionInfoCallback {

    /**
     * Called when graph version available or graph version could not be resolved in time.
     *
     * @param versionInfo Road graph version information. Null if the version is still resolving or
     * if the version could not be identified
     */
    fun onVersionInfo(versionInfo: VersionInfo)

    /**
     * Called when graph version information wasn't received.
     *
     * @param isTimeoutError A flag indicating whether error was due to timeout,
     * passed to [MapboxNavigation.getRoadGraphVersionInfo]. False otherwise.
     */
    fun onError(isTimeoutError: Boolean)

    /**
     * Road graph version information.
     *
     * @property dataset Road graph dataset name
     * @property version Road graph dataset version
     */
    class VersionInfo(
        val dataset: String,
        val version: String,
    ) {

        /**
         * Indicates whether some other object is "equal to" this one.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VersionInfo

            if (dataset != other.dataset) return false
            if (version != other.version) return false

            return true
        }

        /**
         * Returns a hash code value for the object.
         */
        override fun hashCode(): Int {
            var result = dataset.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }

        /**
         * Returns a string representation of the object.
         */
        override fun toString(): String {
            return "VersionInfo(dataset='$dataset', version='$version')"
        }

        internal companion object {

            @JvmSynthetic
            fun createFromNative(
                nativeObj: com.mapbox.navigator.RoadGraphVersionInfo,
            ): VersionInfo {
                return VersionInfo(
                    dataset = nativeObj.dataset,
                    version = nativeObj.version,
                )
            }
        }
    }
}

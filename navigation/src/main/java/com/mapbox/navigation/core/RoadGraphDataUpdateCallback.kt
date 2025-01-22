package com.mapbox.navigation.core

/**
 * Callback to be invoked when the information about available road graph data updates is received.
 * See [MapboxNavigation.requestRoadGraphDataUpdate].
 */
interface RoadGraphDataUpdateCallback {

    /**
     * Invoked when the information about available road graph data updates is received.
     *
     * @param isUpdateAvailable true if new version of road graph data is available, false otherwise.
     * @param versionInfo information about the new version.
     *   Can be null if no information is available.
     */
    fun onRoadGraphDataUpdateInfoAvailable(
        isUpdateAvailable: Boolean,
        versionInfo: RoadGraphVersionInfo?,
    )
}

/**
 * Class containing information about road graph version.
 *
 * @param dataset dataset name
 * @param version version name
 */
class RoadGraphVersionInfo internal constructor(val dataset: String, val version: String) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadGraphVersionInfo

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
        return "RoadGraphVersionInfo(dataset='$dataset', version='$version')"
    }
}

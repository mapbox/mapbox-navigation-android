package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * HistorySearchResult.
 *
 * @property id unique identifier of the search point
 * @property name of the search point
 * @property address of the search point
 * @property coordinates of the search point, null in case of error
 * @property routablePoint [HistoryRoutablePoint]s details, null in case of error or if there is no related routable point
 */
@Keep
@ExperimentalPreviewMapboxNavigationAPI
class HistorySearchResult(
    val id: String,
    val name: String,
    val address: String,
    val coordinates: HistoryPoint?,
    val routablePoint: List<HistoryRoutablePoint>?,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistorySearchResult

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (coordinates != other.coordinates) return false
        return routablePoint == other.routablePoint
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + (coordinates?.hashCode() ?: 0)
        result = 31 * result + (routablePoint?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "HistorySearchResult(" +
            "id='$id', " +
            "name='$name', " +
            "address='$address', " +
            "coordinates=$coordinates, " +
            "routablePoint=$routablePoint" +
            ")"
    }
}

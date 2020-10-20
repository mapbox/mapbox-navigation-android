package com.mapbox.navigation.ui.routealert

import com.mapbox.geojson.Point

/**
 * Data class for [TollCollectionAlertDisplayer].
 *
 * @property coordinate of the toll route alert
 * @property tollDescription give a description to the toll route alert, it shows under the toll icon
 */
class TollCollectionAlertUiModel private constructor(
    val coordinate: Point,
    val tollDescription: String
) {

    /**
     * @return the builder that created the [TollCollectionAlertUiModel]
     */
    fun toBuilder() = Builder(coordinate).apply {
        tollDescription(tollDescription)
    }

    /**
     * Override the equals method. Regenerate whenever a change is made.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TollCollectionAlertUiModel

        if (coordinate != other.coordinate) return false
        if (tollDescription != other.tollDescription) return false

        return true
    }

    /**
     * Override the hashCode method. Regenerate whenever a change is made.
     */
    override fun hashCode(): Int {
        var result = coordinate.hashCode()
        result = 31 * result + tollDescription.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "TollCollectionAlertUiModel(" +
            "coordinate=$coordinate, " +
            "tollDescription=$tollDescription" +
            ")"
    }

    /**
     * Builder for [TollCollectionAlertUiModel]
     */
    class Builder(private val coordinate: Point) {
        private var tollDescription: String = ""

        /**
         * Toll collection alert description
         */
        fun tollDescription(tollDescription: String) = this.apply {
            this.tollDescription = tollDescription
        }

        /**
         * Build the [TollCollectionAlertUiModel]
         */
        fun build() = TollCollectionAlertUiModel(
            coordinate,
            tollDescription
        )
    }
}

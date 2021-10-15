package com.mapbox.navigation.base.trip.model.eh

/**
 * Road name information
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param name road name
 * @param shielded is the road shielded?
 */
class RoadName internal constructor(
    val name: String,
    val shielded: Boolean
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoadName

        if (name != other.name) return false
        if (shielded != other.shielded) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + shielded.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NameInfo(" +
            "name=$name, " +
            "shielded=$shielded" +
            ")"
    }
}

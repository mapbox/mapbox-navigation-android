package com.mapbox.navigation.base.trip.model.eh

/**
 * The position on the current [EHorizon].
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param edgeId the current Edge id
 * @param percentAlong the progress along the current edge [0,1)
 */
class EHorizonGraphPosition internal constructor(
    val edgeId: Long,
    val percentAlong: Double,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonGraphPosition

        if (edgeId != other.edgeId) return false
        if (percentAlong != other.percentAlong) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = edgeId.hashCode()
        result = 31 * result + percentAlong.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonGraphPosition(" +
            "edgeId=$edgeId, " +
            "percentAlong=$percentAlong" +
            ")"
    }
}

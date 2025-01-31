package com.mapbox.navigation.base.trip.model.eh

/**
 * EHorizonPosition
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
 *
 * @param eHorizonGraphPosition current graph position
 * @param eHorizon tree of edges
 * @param eHorizonResultType result type. see [EHorizonResultType]
 */
class EHorizonPosition internal constructor(
    val eHorizonGraphPosition: EHorizonGraphPosition,
    val eHorizon: EHorizon,
    @EHorizonResultType.Type val eHorizonResultType: String,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonPosition

        if (eHorizonGraphPosition != other.eHorizonGraphPosition) return false
        if (eHorizon != other.eHorizon) return false
        if (eHorizonResultType != other.eHorizonResultType) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = eHorizonGraphPosition.hashCode()
        result = 31 * result + eHorizon.hashCode()
        result = 31 * result + eHorizonResultType.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "EHorizonPosition(" +
            "eHorizonGraphPosition=$eHorizonGraphPosition, " +
            "eHorizon=$eHorizon, " +
            "eHorizonResultType=$eHorizonResultType" +
            ")"
    }
}

package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions

/**
 * A simplified data structure representing a single [BannerInstructions]
 * @property primary PrimaryManeuver represents [BannerInstructions.primary]
 * @property totalManeuverDistance TotalManeuverDistance represents [BannerInstructions.distanceAlongGeometry]
 * @property secondary SecondaryManeuver? represents [BannerInstructions.secondary]
 * @property sub SubManeuver? represents [BannerInstructions.sub] with type text
 * @property laneGuidance Lane? represents [BannerInstructions.sub] with type lane
 */
class Maneuver private constructor(
    val primary: PrimaryManeuver,
    val totalManeuverDistance: TotalManeuverDistance,
    val secondary: SecondaryManeuver?,
    val sub: SubManeuver?,
    val laneGuidance: Lane?
) {
    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Maneuver

        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (sub != other.sub) return false
        if (laneGuidance != other.laneGuidance) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + totalManeuverDistance.hashCode()
        result = 31 * result + (secondary?.hashCode() ?: 0)
        result = 31 * result + (sub?.hashCode() ?: 0)
        result = 31 * result + (laneGuidance?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "Maneuver(primary=$primary, " +
            "totalManeuverDistance=$totalManeuverDistance, " +
            "secondary=$secondary, " +
            "sub=$sub, " +
            "laneGuidance=$laneGuidance" +
            ")"
    }

    /**
     * @return Builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .primary(primary)
            .totalManeuverDistance(totalManeuverDistance)
            .secondary(secondary)
            .sub(sub)
            .laneGuidance(laneGuidance)
    }

    /**
     * Build a new [Maneuver]
     * @property primary PrimaryManeuver
     * @property totalManeuverDistance TotalManeuverDistance
     * @property secondary SecondaryManeuver?
     * @property sub SubManeuver?
     * @property laneGuidance Lane?
     */
    class Builder {
        private var primary: PrimaryManeuver = PrimaryManeuver.Builder().build()
        private var totalManeuverDistance: TotalManeuverDistance = TotalManeuverDistance(0.0)
        private var secondary: SecondaryManeuver? = null
        private var sub: SubManeuver? = null
        private var laneGuidance: Lane? = null

        /**
         * apply primary to Builder
         * @param primary PrimaryManeuver
         * @return Builder
         */
        fun primary(primary: PrimaryManeuver): Builder =
            apply { this.primary = primary }

        /**
         * apply totalManeuverDistance to Builder
         * @param totalManeuverDistance TotalManeuverDistance
         * @return Builder
         */
        fun totalManeuverDistance(totalManeuverDistance: TotalManeuverDistance): Builder =
            apply { this.totalManeuverDistance = totalManeuverDistance }

        /**
         * apply secondary to Builder
         * @param secondary SecondaryManeuver
         * @return Builder
         */
        fun secondary(secondary: SecondaryManeuver?): Builder =
            apply { this.secondary = secondary }

        /**
         * apply sub to Builder
         * @param sub SubManeuver
         * @return Builder
         */
        fun sub(sub: SubManeuver?): Builder =
            apply { this.sub = sub }

        /**
         * apply laneGuidance to Builder
         * @param laneGuidance Lane
         * @return Builder
         */
        fun laneGuidance(laneGuidance: Lane?): Builder =
            apply { this.laneGuidance = laneGuidance }

        /**
         * Build the [Maneuver]
         * @return Maneuver
         */
        fun build(): Maneuver {
            return Maneuver(primary, totalManeuverDistance, secondary, sub, laneGuidance)
        }
    }
}

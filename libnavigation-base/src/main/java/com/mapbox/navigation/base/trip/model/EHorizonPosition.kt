package com.mapbox.navigation.base.trip.model

/**
 * The position on the current [EHorizon].
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * @param edgeId the current Edge id
 * @param percentAlong the progress along the current edge [0,1)
 */
class EHorizonPosition private constructor(
    val edgeId: Long,
    val percentAlong: Double
) {

    /**
     * @return the builder that created the [EHorizon]
     */
    fun toBuilder(): Builder = Builder().apply {
        edgeId(edgeId)
        percentAlong(percentAlong)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EHorizonPosition

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
        return "EHorizonPosition(" +
            "edgeId=$edgeId, " +
            "percentAlong=$percentAlong" +
            ")"
    }

    /**
     * Builder for [EHorizon].
     */
    class Builder {

        private var edgeId: Long = 0
        private var percentAlong: Double = 0.0

        /**
         * Defines the current [Edge] id
         */
        fun edgeId(edgeId: Long): Builder =
            apply { this.edgeId = edgeId }

        /**
         * Defines the progress along the current [Edge]
         */
        fun percentAlong(percentAlong: Double): Builder =
            apply { this.percentAlong = percentAlong }

        /**
         * Build the [EHorizonPosition]
         */
        fun build(): EHorizonPosition {
            return EHorizonPosition(
                edgeId = edgeId,
                percentAlong = percentAlong
            )
        }
    }
}

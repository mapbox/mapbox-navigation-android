package com.mapbox.navigation.base.options

/**
 * Alerts service extracts information from the road graph and helps track it through eHorizon.
 * [AlertServiceOptions] control how Navigation SDK extracts road objects from the road graph and which objects are collected.
 * Some objects may take significant effort to extract and thus there's an option to disable collection of some of the types.
 *
 * @param collectTunnels whether Tunnels should be collected
 * @param collectBridges whether Bridges should be collected
 * @param collectRestrictedAreas whether Restricted Areas should be collected
 * @param collectMergingAreas whether Merging Areas should be collected
 */
class AlertServiceOptions private constructor(
    val collectTunnels: Boolean,
    val collectBridges: Boolean,
    val collectRestrictedAreas: Boolean,
    val collectMergingAreas: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        collectTunnels(collectTunnels)
        collectBridges(collectBridges)
        collectRestrictedAreas(collectRestrictedAreas)
        collectMergingAreas(collectMergingAreas)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlertServiceOptions

        if (collectTunnels != other.collectTunnels) return false
        if (collectBridges != other.collectBridges) return false
        if (collectRestrictedAreas != other.collectRestrictedAreas) return false
        if (collectMergingAreas != other.collectMergingAreas) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = collectTunnels.hashCode()
        result = 31 * result + collectBridges.hashCode()
        result = 31 * result + collectRestrictedAreas.hashCode()
        result = 31 * result + collectMergingAreas.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AlertServiceOptions(" +
            "collectTunnels='$collectTunnels', " +
            "collectBridges='$collectBridges', " +
            "collectRestrictedAreas='$collectRestrictedAreas', " +
            "collectMergingAreas='$collectMergingAreas'" +
            ")"
    }

    /**
     * Build a new [AlertServiceOptions].
     */
    class Builder {
        private var collectTunnels: Boolean = true
        private var collectBridges: Boolean = true
        private var collectRestrictedAreas: Boolean = false
        private var collectMergingAreas: Boolean = false

        /**
         * Set whether Tunnels should be collected defaults to `true`.
         */
        fun collectTunnels(collectTunnels: Boolean): Builder = apply {
            this.collectTunnels = collectTunnels
        }

        /**
         * Set whether Bridges should be collected defaults to `true`.
         */
        fun collectBridges(collectBridges: Boolean): Builder = apply {
            this.collectBridges = collectBridges
        }

        /**
         * Set whether Restricted Areas should be collected defaults to `false`.
         */
        fun collectRestrictedAreas(collectRestrictedAreas: Boolean): Builder = apply {
            this.collectRestrictedAreas = collectRestrictedAreas
        }

        /**
         * Set whether Merging Areas should be collected defaults to `false`.
         */
        fun collectMergingAreas(collectMergingAreas: Boolean): Builder = apply {
            this.collectMergingAreas = collectMergingAreas
        }

        /**
         * Build the [AlertServiceOptions]
         */
        fun build(): AlertServiceOptions {
            return AlertServiceOptions(
                collectTunnels = collectTunnels,
                collectBridges = collectBridges,
                collectRestrictedAreas = collectRestrictedAreas,
                collectMergingAreas = collectMergingAreas,
            )
        }
    }
}

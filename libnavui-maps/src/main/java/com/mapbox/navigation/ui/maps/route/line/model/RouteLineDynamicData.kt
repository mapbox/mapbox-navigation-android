package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Provides information needed to draw a route.
 *
 * @param baseExpressionProvider expression used to style the base of the line
 * @param casingExpressionProvider expression used to style the case of the line
 * @param trafficExpressionProvider expression used to style the congestion colors on the line
 * @param restrictedSectionExpressionProvider expression used to style the restricted sections on the line
 * @param violatedSectionExpressionProvider expression used to style the violated sections on the line
 * @param trimOffset a value representing the section of the line that should be trimmed and made transparent. Null by default
 * @param trailExpressionProvider expression used to style the trail layer
 * @param trailCasingExpressionProvider expression used to style the trail casing layer
 */
class RouteLineDynamicData internal constructor(
    val baseExpressionProvider: RouteLineExpressionProvider,
    val casingExpressionProvider: RouteLineExpressionProvider,
    val trafficExpressionProvider: RouteLineExpressionProvider?,
    val restrictedSectionExpressionProvider: RouteLineExpressionProvider?,
    val violatedSectionExpressionProvider: RouteLineExpressionProvider?,
    val trimOffset: RouteLineTrimOffset? = null,
    val trailExpressionProvider: RouteLineExpressionProvider? = null,
    val trailCasingExpressionProvider: RouteLineExpressionProvider? = null
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteLineDynamicData(
        baseExpressionProvider,
        casingExpressionProvider,
        trafficExpressionProvider,
        restrictedSectionExpressionProvider,
        violatedSectionExpressionProvider,
        trimOffset,
        trailExpressionProvider,
        trailCasingExpressionProvider
    )

    /**
     * Provides a mutable representation of information needed to draw a route.
     *
     * @param baseExpressionProvider expression used to style the base of the line
     * @param casingExpressionProvider expression used to style the case of the line
     * @param trafficExpressionProvider expression used to style the congestion colors on the line
     * @param restrictedSectionExpressionProvider expression used to style the restricted sections on the line
     * @param violatedSectionExpressionProvider expression used to style the violated sections on the line
     * @param trimOffset a value representing the section of the line that should be trimmed and made transparent. Null by default
     * @param trailExpressionProvider expression used to style the trail layer
     * @param trailCasingExpressionProvider expression used to style the trail casing layer
     */
    class MutableRouteLineDynamicData internal constructor(
        var baseExpressionProvider: RouteLineExpressionProvider,
        var casingExpressionProvider: RouteLineExpressionProvider,
        var trafficExpressionProvider: RouteLineExpressionProvider?,
        var restrictedSectionExpressionProvider: RouteLineExpressionProvider?,
        var violatedSectionExpressionProvider: RouteLineExpressionProvider?,
        var trimOffset: RouteLineTrimOffset? = null,
        var trailExpressionProvider: RouteLineExpressionProvider? = null,
        var trailCasingExpressionProvider: RouteLineExpressionProvider? = null
    ) {

        /**
         * @return a RouteLineDynamicData
         */
        fun toImmutableValue() = RouteLineDynamicData(
            baseExpressionProvider,
            casingExpressionProvider,
            trafficExpressionProvider,
            restrictedSectionExpressionProvider,
            violatedSectionExpressionProvider,
            trimOffset,
            trailExpressionProvider,
            trailCasingExpressionProvider
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineDynamicData

        if (baseExpressionProvider != other.baseExpressionProvider) return false
        if (casingExpressionProvider != other.casingExpressionProvider) return false
        if (trafficExpressionProvider != other.trafficExpressionProvider) return false
        if (restrictedSectionExpressionProvider != other.restrictedSectionExpressionProvider) {
            return false
        }
        if (violatedSectionExpressionProvider != other.violatedSectionExpressionProvider) {
            return false
        }
        if (trimOffset != other.trimOffset) return false
        if (trailExpressionProvider != other.trailExpressionProvider) return false
        if (trailCasingExpressionProvider != other.trailCasingExpressionProvider) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = baseExpressionProvider.hashCode()
        result = 31 * result + casingExpressionProvider.hashCode()
        result = 31 * result + (trafficExpressionProvider?.hashCode() ?: 0)
        result = 31 * result + (restrictedSectionExpressionProvider?.hashCode() ?: 0)
        result = 31 * result + (violatedSectionExpressionProvider?.hashCode() ?: 0)
        result = 31 * result + (trimOffset?.hashCode() ?: 0)
        result = 31 * result + (trailExpressionProvider?.hashCode() ?: 0)
        result = 31 * result + (trailCasingExpressionProvider?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineDynamicData(" +
            "baseExpressionProvider=$baseExpressionProvider, " +
            "casingExpressionProvider=$casingExpressionProvider, " +
            "trafficExpressionProvider=$trafficExpressionProvider, " +
            "restrictedSectionExpressionProvider=$restrictedSectionExpressionProvider," +
            "violatedSectionExpressionProvider=$violatedSectionExpressionProvider," +
            "trimOffset=$trimOffset," +
            "trailExpressionProvider=$trailExpressionProvider," +
            "trailCasingExpressionProvider=$trailCasingExpressionProvider," +
            ")"
    }
}

package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 * * @param slopeStep if true, slopeStep type will be generated
 * @param slopeLinear if true, slopeLinear type will be generated
 * @param curvature if true, curvature type will be generated
 * @param routeNumTypes if true, routeNumTypes type will be generated
 * @param roadCondition if true, roadCondition type will be generated
 * @param roadAccessibility if true, roadAccessibility type will be generated
 * @param variableSpeedSign if true, variableSpeedSign type will be generated
 * @param headingChange if true, headingChange type will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigProfileShortTypeOptions(
    val slopeStep: Boolean,
    val slopeLinear: Boolean,
    val curvature: Boolean,
    val routeNumTypes: Boolean,
    val roadCondition: Boolean,
    val roadAccessibility: Boolean,
    val variableSpeedSign: Boolean,
    val headingChange: Boolean,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigProfileShortTypeOptions():
        com.mapbox.navigator.AdasisConfigProfileshortTypeOptions {
        return com.mapbox.navigator.AdasisConfigProfileshortTypeOptions(
            slopeStep,
            slopeLinear,
            curvature,
            routeNumTypes,
            roadCondition,
            roadAccessibility,
            variableSpeedSign,
            headingChange
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdasisConfigProfileShortTypeOptions

        if (slopeStep != other.slopeStep) return false
        if (slopeLinear != other.slopeLinear) return false
        if (curvature != other.curvature) return false
        if (routeNumTypes != other.routeNumTypes) return false
        if (roadCondition != other.roadCondition) return false
        if (roadAccessibility != other.roadAccessibility) return false
        if (variableSpeedSign != other.variableSpeedSign) return false
        if (headingChange != other.headingChange) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = slopeStep.hashCode()
        result = 31 * result + slopeLinear.hashCode()
        result = 31 * result + curvature.hashCode()
        result = 31 * result + routeNumTypes.hashCode()
        result = 31 * result + roadCondition.hashCode()
        result = 31 * result + roadAccessibility.hashCode()
        result = 31 * result + variableSpeedSign.hashCode()
        result = 31 * result + headingChange.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "AdasisConfigProfileShortTypeOptions(" +
            "slopeStep=$slopeStep, " +
            "slopeLinear=$slopeLinear, " +
            "curvature=$curvature, " +
            "routeNumTypes=$routeNumTypes, " +
            "roadCondition=$roadCondition, " +
            "roadAccessibility=$roadAccessibility, " +
            "variableSpeedSign=$variableSpeedSign, " +
            "headingChange=$headingChange" +
            ")"
    }
}

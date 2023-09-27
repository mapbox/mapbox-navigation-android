package com.mapbox.navigation.core.adasis

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 *
 * @param slopeStep if true, slopeStep type will be generated
 * @param curvature if true, curvature type will be generated
 * @param roadCondition if true, roadCondition type will be generated
 * @param variableSpeedSign if true, variableSpeedSign type will be generated
 * @param headingChange if true, headingChange type will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
class AdasisConfigProfileShortTypeOptions(
    val slopeStep: Boolean = false,
    val curvature: Boolean = true,
    val roadCondition: Boolean = true,
    val variableSpeedSign: Boolean = false,
    val headingChange: Boolean = true,
) {

    @JvmSynthetic
    internal fun toNativeAdasisConfigProfileShortTypeOptions():
        com.mapbox.navigator.AdasisConfigProfileshortTypeOptions {
        return com.mapbox.navigator.AdasisConfigProfileshortTypeOptions(
            slopeStep,
            curvature,
            roadCondition,
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
        if (curvature != other.curvature) return false
        if (roadCondition != other.roadCondition) return false
        if (variableSpeedSign != other.variableSpeedSign) return false
        if (headingChange != other.headingChange) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = slopeStep.hashCode()
        result = 31 * result + curvature.hashCode()
        result = 31 * result + roadCondition.hashCode()
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
            "curvature=$curvature, " +
            "roadCondition=$roadCondition, " +
            "variableSpeedSign=$variableSpeedSign, " +
            "headingChange=$headingChange" +
            ")"
    }
}

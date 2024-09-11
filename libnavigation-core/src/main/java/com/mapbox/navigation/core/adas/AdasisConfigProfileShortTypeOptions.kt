package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Profile short message options
 *
 * @param slopeStep if true, slopeStep type will be generated
 * @param curvature if true, curvature type will be generated
 * @param roadCondition if true, roadCondition type will be generated
 * @param variableSpeedSign if true, variableSpeedSign type will be generated
 * @param headingChange if true, headingChange type will be generated
 * @param historyAverageSpeed if true, historyAverageSpeed type will be generated
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class AdasisConfigProfileShortTypeOptions private constructor(
    val slopeStep: Boolean,
    val curvature: Boolean,
    val roadCondition: Boolean,
    val variableSpeedSign: Boolean,
    val headingChange: Boolean,
    val historyAverageSpeed: Boolean,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder()
        .slopeStep(slopeStep)
        .curvature(curvature)
        .roadCondition(roadCondition)
        .variableSpeedSign(variableSpeedSign)
        .headingChange(headingChange)
        .historyAverageSpeed(historyAverageSpeed)

    @JvmSynthetic
    internal fun toNativeAdasisConfigProfileShortTypeOptions():
        com.mapbox.navigator.AdasisConfigProfileshortTypeOptions {
        return com.mapbox.navigator.AdasisConfigProfileshortTypeOptions(
            slopeStep,
            curvature,
            roadCondition,
            variableSpeedSign,
            headingChange,
            historyAverageSpeed,
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
        return historyAverageSpeed == other.historyAverageSpeed
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
        result = 31 * result + historyAverageSpeed.hashCode()
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
            "headingChange=$headingChange, " +
            "historyAverageSpeed=$historyAverageSpeed" +
            ")"
    }

    /**
     * Builder for [AdasisConfigProfileShortTypeOptions].
     */
    class Builder {

        private var slopeStep: Boolean = false
        private var curvature: Boolean = true
        private var roadCondition: Boolean = true
        private var variableSpeedSign: Boolean = false
        private var headingChange: Boolean = true
        private var historyAverageSpeed: Boolean = true

        /**
         * If true, slopeStep type will be generated
         */
        fun slopeStep(slopeStep: Boolean): Builder = apply {
            this.slopeStep = slopeStep
        }

        /**
         * If true, curvature type will be generated
         */
        fun curvature(curvature: Boolean): Builder = apply {
            this.curvature = curvature
        }

        /**
         * If true, roadCondition type will be generated
         */
        fun roadCondition(roadCondition: Boolean): Builder = apply {
            this.roadCondition = roadCondition
        }

        /**
         * If true, variableSpeedSign type will be generated
         */
        fun variableSpeedSign(variableSpeedSign: Boolean): Builder = apply {
            this.variableSpeedSign = variableSpeedSign
        }

        /**
         * If true, headingChange type will be generated
         */
        fun headingChange(headingChange: Boolean): Builder = apply {
            this.headingChange = headingChange
        }

        /**
         * If true, historyAverageSpeed type will be generated
         */
        fun historyAverageSpeed(historyAverageSpeed: Boolean): Builder = apply {
            this.historyAverageSpeed = historyAverageSpeed
        }

        /**
         * Build the [AdasisConfigProfileShortTypeOptions]
         */
        fun build() = AdasisConfigProfileShortTypeOptions(
            slopeStep = slopeStep,
            curvature = curvature,
            roadCondition = roadCondition,
            variableSpeedSign = variableSpeedSign,
            headingChange = headingChange,
            historyAverageSpeed = historyAverageSpeed,
        )
    }
}

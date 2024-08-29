package com.mapbox.navigation.base.physics

import androidx.annotation.IntRange
import com.mapbox.navigation.base.speed.model.SpeedUnit
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Represents a finite speed of a vehicle.
 *
 * The type stores speed values in a km/h up to [Double.MAX_VALUE] rounded to hundred-thousandths (.00001).
 *
 * To construct a speed use either the extension function [toSpeed], or the extension
 * properties [kph], [mph] or [m_s] available on all [Number] types.
 *
 * To get the value of this speed expressed in a particular [speed unit][SpeedUnit]
 * use the functions [toInt], [toLong], [toDouble] and [toFloat].
 */
class Speed private constructor(valueKph: Double) : Comparable<Speed> {

    private val rawValueKph: Double = valueKph.roundToExactDecimals(5)

    companion object {

        /** The speed equal to exactly 0 km/h. */
        val ZERO = Speed(0.0)

        /** Returns a [Speed] equal to this [Number] of the specified [unit]. */
        fun Number.toSpeed(unit: SpeedUnit) = when (unit) {
            SpeedUnit.KILOMETERS_PER_HOUR -> Speed(this.toDouble())
            SpeedUnit.MILES_PER_HOUR -> Speed(mphToKph(toDouble()))
            SpeedUnit.METERS_PER_SECOND -> Speed(metersPerSecToKph(toDouble()))
        }

        /** Returns a [Speed] equal to this [Number] of kilometers per hour */
        val Number.kph: Speed get() = toSpeed(SpeedUnit.KILOMETERS_PER_HOUR)

        /** Returns a [Speed] equal to this [Number] of miles per hour */
        val Number.mph: Speed get() = toSpeed(SpeedUnit.MILES_PER_HOUR)

        /** Returns a [Speed] equal to this [Number] of meters per second */
        val Number.m_s: Speed get() = toSpeed(SpeedUnit.METERS_PER_SECOND)
    }

    /** Returns the negative of this value. */
    operator fun unaryMinus(): Speed = Speed(-rawValueKph)

    /**
     * Returns a speed whose value is the sum of this and [other] speed value.
     */
    operator fun plus(other: Speed): Speed {
        val speedKph = rawValueKph + other.rawValueKph
        return Speed(speedKph.coerceIn(-MAX_SPEED_KPH, MAX_SPEED_KPH))
    }

    /**
     * Returns a speed whose value is the difference between this and [other] speed value.
     */
    operator fun minus(other: Speed): Speed = this + (-other)

    /**
     * Returns a speed whose value is this speed value multiplied by the given [scale] number.
     */
    operator fun times(scale: Number): Speed {
        val speedKph = rawValueKph * scale.toDouble()
        return Speed(speedKph.coerceIn(-MAX_SPEED_KPH, MAX_SPEED_KPH))
    }

    /**
     * Returns a speed whose value is this speed value divided by the given [scale] number.
     */
    operator fun div(scale: Number): Speed {
        require(scale != 0.0) { "Dividing by zero yields an undefined result." }
        return Speed(rawValueKph / scale.toDouble())
    }

    /**
     * Returns a number that is the ratio of this and [other] speed value.
     */
    operator fun div(other: Speed): Double = rawValueKph / other.rawValueKph

    /**
     * Compares this object with the specified object for order.
     * Returns zero if this object is equal to the specified other object,
     * a negative number if it's less than other, or a positive number if it's greater than other.
     */
    override fun compareTo(other: Speed): Int = rawValueKph.compareTo(other.rawValueKph)

    /**
     * Returns a string representation of this speed value in [SpeedUnit.KILOMETERS_PER_HOUR].
     */
    override fun toString(): String = toString(SpeedUnit.KILOMETERS_PER_HOUR)

    /**
     * Returns a string representation of this speed value expressed in the given [unit].
     *
     * @return the value of speed in the specified [unit] followed by that unit abbreviated
     * name: `km/h`, `MPH` or `m/s`
     */
    fun toString(unit: SpeedUnit): String = when (unit) {
        SpeedUnit.KILOMETERS_PER_HOUR -> "$rawValueKph ${unit.abbr}"
        SpeedUnit.MILES_PER_HOUR -> "${kphToMph(rawValueKph)} ${unit.abbr}"
        SpeedUnit.METERS_PER_SECOND -> "${kphToMetersPerSec(rawValueKph)} ${unit.abbr}"
    }

    /**
     * Returns the value of this speed expressed as an [Int] number of the specified [unit].
     */
    fun toInt(unit: SpeedUnit): Int = when (unit) {
        SpeedUnit.KILOMETERS_PER_HOUR -> rawValueKph.toInt()
        SpeedUnit.MILES_PER_HOUR -> kphToMph(rawValueKph).toInt()
        SpeedUnit.METERS_PER_SECOND -> kphToMetersPerSec(rawValueKph).toInt()
    }

    /**
     * Returns the value of this speed expressed as an [Long] number of the specified [unit].
     */
    fun toLong(unit: SpeedUnit): Long = when (unit) {
        SpeedUnit.KILOMETERS_PER_HOUR -> rawValueKph.toLong()
        SpeedUnit.MILES_PER_HOUR -> kphToMph(rawValueKph).toLong()
        SpeedUnit.METERS_PER_SECOND -> kphToMetersPerSec(rawValueKph).toLong()
    }

    /**
     * Returns the value of this speed expressed as an [Int] number of the specified [unit]
     * rounded to the nearest integer.
     */
    fun roundToInt(unit: SpeedUnit): Int = toDouble(unit).roundToInt()

    /**
     * Returns the value of this speed expressed as an [Long] number of the specified [unit]
     * rounded to the nearest integer.
     */
    fun roundToLong(unit: SpeedUnit): Long = toDouble(unit).roundToLong()

    /**
     * Returns the value of this speed expressed as an [Double] number of the specified [unit].
     */
    fun toDouble(unit: SpeedUnit): Double = when (unit) {
        SpeedUnit.KILOMETERS_PER_HOUR -> rawValueKph
        SpeedUnit.MILES_PER_HOUR -> kphToMph(rawValueKph)
        SpeedUnit.METERS_PER_SECOND -> kphToMetersPerSec(rawValueKph)
    }

    /**
     * Returns the value of this speed expressed as an [Float] number of the specified [unit].
     */
    fun toFloat(unit: SpeedUnit): Float = when (unit) {
        SpeedUnit.KILOMETERS_PER_HOUR -> rawValueKph.toFloat()
        SpeedUnit.MILES_PER_HOUR -> kphToMph(rawValueKph).toFloat()
        SpeedUnit.METERS_PER_SECOND -> kphToMetersPerSec(rawValueKph).toFloat()
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Speed

        return rawValueKph == other.rawValueKph
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int = rawValueKph.hashCode()
}

internal val SpeedUnit.abbr
    get(): String = when (this) {
        SpeedUnit.KILOMETERS_PER_HOUR -> "km/h"
        SpeedUnit.MILES_PER_HOUR -> "MPH"
        SpeedUnit.METERS_PER_SECOND -> "m/s"
    }

internal fun Double.roundToExactDecimals(@IntRange(0, 6) decimals: Int): Double {
    require(0 <= decimals) { "decimals must be not negative, but was $decimals" }
    val factor = 10.0.pow(decimals)
    return (factor * this).roundToInt() / factor
}

internal fun mphToKph(mph: Double): Double = mph * 1.609344
internal fun kphToMph(kph: Double): Double = kph / 1.609344
internal fun metersPerSecToKph(metersPerSec: Double): Double = metersPerSec * 3.6
internal fun kphToMetersPerSec(kph: Double): Double = kph / 3.6

internal const val MAX_SPEED_KPH = Double.MAX_VALUE

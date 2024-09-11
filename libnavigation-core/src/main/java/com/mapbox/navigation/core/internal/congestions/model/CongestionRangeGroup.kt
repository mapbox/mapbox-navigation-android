package com.mapbox.navigation.core.internal.congestions.model

internal class CongestionRangeGroup(
    internal val low: IntRange,
    internal val moderate: IntRange,
    internal val heavy: IntRange,
    internal val severe: IntRange,
) {
    fun fromCongestionSeverityType(type: CongestionSeverityType): IntRange {
        return when (type) {
            CongestionSeverityType.LOW -> low
            CongestionSeverityType.MODERATE -> moderate
            CongestionSeverityType.HEAVY -> heavy
            CongestionSeverityType.SEVERE -> severe
        }
    }

    internal fun fromCongestionValue(congestion: Int): CongestionSeverityType {
        return when (congestion) {
            in low -> CongestionSeverityType.LOW
            in moderate -> CongestionSeverityType.MODERATE
            in heavy -> CongestionSeverityType.HEAVY
            in severe -> CongestionSeverityType.SEVERE
            else -> CongestionSeverityType.LOW
        }
    }
}

internal enum class CongestionSeverityType(val weight: Int) {
    LOW(1),
    MODERATE(2),
    HEAVY(3),
    SEVERE(4),
    ;

    companion object {
        fun fromWeightValue(weight: Int): CongestionSeverityType {
            return CongestionSeverityType.values().firstOrNull {
                it.weight == weight
            } ?: if (weight < 1) LOW else SEVERE
        }
    }
}

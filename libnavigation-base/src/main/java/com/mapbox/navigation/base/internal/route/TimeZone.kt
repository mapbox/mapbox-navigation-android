package com.mapbox.navigation.base.internal.route

class TimeZone internal constructor(
    val offset: String,
    val identifier: String,
    val abbreviation: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeZone

        if (offset != other.offset) return false
        if (identifier != other.identifier) return false
        if (abbreviation != other.abbreviation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + identifier.hashCode()
        result = 31 * result + abbreviation.hashCode()
        return result
    }

    override fun toString(): String {
        return "TimeZone(offset='$offset'" +
            ", identifier='$identifier'" +
            ", abbreviation='$abbreviation'" +
            ")"
    }
}

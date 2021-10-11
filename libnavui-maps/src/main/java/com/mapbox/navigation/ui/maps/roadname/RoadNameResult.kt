package com.mapbox.navigation.ui.maps.roadname

internal sealed class RoadNameResult {
    data class RoadNameLabel(
        val name: String?,
        val shield: ByteArray?,
        val shieldName: String?
    ) : RoadNameResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RoadNameLabel

            if (name != other.name) return false
            if (shield != null) {
                if (other.shield == null) return false
                if (!shield.contentEquals(other.shield)) return false
            } else if (other.shield != null) return false
            if (shieldName != other.shieldName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + (shield?.contentHashCode() ?: 0)
            result = 31 * result + (shieldName?.hashCode() ?: 0)
            return result
        }
    }
}

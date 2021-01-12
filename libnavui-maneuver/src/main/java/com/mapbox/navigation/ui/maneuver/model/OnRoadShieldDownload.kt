package com.mapbox.navigation.ui.maneuver.model

internal data class OnRoadShieldDownload(
    val data: ByteArray?,
    val exception: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OnRoadShieldDownload

        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (exception != other.exception) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data?.contentHashCode() ?: 0
        result = 31 * result + (exception?.hashCode() ?: 0)
        return result
    }
}

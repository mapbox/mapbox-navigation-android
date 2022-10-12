package com.mapbox.navigation.ui.maps.route.line.model

/**
 * @param offset the percentage of the distance traveled along the route from the origin.
 * @param legIndex the route leg index for this data
 */
internal abstract class ExpressionOffsetData(
    val offset: Double,
    val legIndex: Int,
) {

    abstract fun <T : ExpressionOffsetData> copyWithNewOffset(newOffset: Double): T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExpressionOffsetData

        if (offset != other.offset) return false
        if (legIndex != other.legIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + legIndex
        return result
    }

    override fun toString(): String {
        return "ExpressionOffsetData(offset=$offset, legIndex=$legIndex)"
    }
}

package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.CurrentIndicesSnapshot
import com.mapbox.navigation.core.directions.session.RoutesExtra

internal sealed class SetRoutesInfo(
    @RoutesExtra.RoutesUpdateReason val reason: String,
    val legIndex: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetRoutesInfo

        if (reason != other.reason) return false
        if (legIndex != other.legIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = reason.hashCode()
        result = 31 * result + legIndex
        return result
    }

    override fun toString(): String {
        return "SetRoutesInfo(reason=$reason, legIndex=$legIndex)"
    }
}

internal class BasicSetRoutesInfo(
    @RoutesExtra.RoutesUpdateReason reason: String,
    legIndex: Int = 0,
) : SetRoutesInfo(reason, legIndex) {
    override fun toString(): String {
        return "BasicSetRoutesInfo() ${super.toString()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }
}

internal class SetAlternativeRoutesInfo(legIndex: Int) : SetRoutesInfo(
    RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
    legIndex
) {
    override fun toString(): String {
        return "SetAlternativeRoutesInfo() ${super.toString()}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        return true
    }
}

internal class SetRefreshedRoutesInfo(
    val currentIndicesSnapshot: CurrentIndicesSnapshot
) : SetRoutesInfo(RoutesExtra.ROUTES_UPDATE_REASON_REFRESH, currentIndicesSnapshot.legIndex) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SetRefreshedRoutesInfo

        if (currentIndicesSnapshot != other.currentIndicesSnapshot) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + currentIndicesSnapshot.hashCode()
        return result
    }

    override fun toString(): String {
        return "SetRefreshedRoutesInfo(" +
            "currentIndicesSnapshot=$currentIndicesSnapshot) " +
            super.toString() +
            ""
    }
}

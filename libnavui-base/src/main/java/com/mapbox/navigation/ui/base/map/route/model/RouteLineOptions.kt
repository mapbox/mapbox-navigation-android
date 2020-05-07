package com.mapbox.navigation.ui.base.map.route.model

import android.graphics.Color
import androidx.annotation.ColorInt

data class RouteLineOptions(
    val routeLayerPosition: RouteLayerPosition?,
    val roundedLineCap: Boolean,
    val primaryRouteIndex: Int,
    val primaryRouteVisible: Boolean,
    @ColorInt val primaryRouteColor: Int,
    val primaryRouteScale: Double,
    @ColorInt val primaryShieldColor: Int,
    @ColorInt val primaryCongestionLightColor: Int,
    @ColorInt val primaryCongestionModerateColor: Int,
    @ColorInt val primaryCongestionHeavyColor: Int,
    @ColorInt val primaryCongestionSevereColor: Int,
    val alternativeRouteVisible: Boolean,
    @ColorInt val alternativeRouteColor: Int,
    val alternativeRouteScale: Double,
    @ColorInt val alternativeShieldColor: Int,
    @ColorInt val alternativeCongestionLightColor: Int,
    @ColorInt val alternativeCongestionModerateColor: Int,
    @ColorInt val alternativeCongestionHeavyColor: Int,
    @ColorInt val alternativeCongestionSevereColor: Int
) {

    fun toBuilder() = Builder(
        routeLayerPosition,
        roundedLineCap,
        primaryRouteIndex,
        primaryRouteVisible,
        primaryRouteColor,
        primaryRouteScale,
        primaryShieldColor,
        primaryCongestionLightColor,
        primaryCongestionModerateColor,
        primaryCongestionHeavyColor,
        primaryCongestionSevereColor,
        alternativeRouteVisible,
        alternativeRouteColor,
        alternativeRouteScale,
        alternativeShieldColor,
        alternativeCongestionLightColor,
        alternativeCongestionModerateColor,
        alternativeCongestionHeavyColor,
        alternativeCongestionSevereColor
    )

    data class Builder(
        private var routeLayerPosition: RouteLayerPosition? = null,
        private var roundedLineCap: Boolean = true,
        private var primaryRouteIndex: Int = 0,
        private var primaryRouteVisible: Boolean = true,
        private var primaryRouteColor: Int = Color.BLACK,
        private var primaryRouteScale: Double = 1.0,
        private var primaryShieldColor: Int = Color.BLACK,
        private var primaryCongestionLightColor: Int = Color.BLACK,
        private var primaryCongestionModerateColor: Int = Color.BLACK,
        private var primaryCongestionHeavyColor: Int = Color.BLACK,
        private var primaryCongestionSevereColor: Int = Color.BLACK,
        private var alternativeRouteVisible: Boolean = true,
        private var alternativeRouteColor: Int = Color.BLACK,
        private var alternativeRouteScale: Double = 1.0,
        private var alternativeShieldColor: Int = Color.BLACK,
        private var alternativeCongestionLightColor: Int = Color.BLACK,
        private var alternativeCongestionModerateColor: Int = Color.BLACK,
        private var alternativeCongestionHeavyColor: Int = Color.BLACK,
        private var alternativeCongestionSevereColor: Int = Color.BLACK
    ) {

        fun routeLayerPosition(routeLayerPosition: RouteLayerPosition?) =
            apply { this.routeLayerPosition = routeLayerPosition }

        fun roundedLineCap(roundedLineCap: Boolean) =
            apply { this.roundedLineCap = roundedLineCap }

        fun primaryRouteIndex(primaryRouteIndex: Int) =
            apply { this.primaryRouteIndex = primaryRouteIndex }

        fun primaryRouteVisible(primaryRouteVisible: Boolean) =
            apply { this.primaryRouteVisible = primaryRouteVisible }

        fun primaryRouteColor(@ColorInt primaryRouteColor: Int) =
            apply { this.primaryRouteColor = primaryRouteColor }

        fun primaryRouteScale(primaryRouteScale: Double) =
            apply { this.primaryRouteScale = primaryRouteScale }

        fun primaryShieldColor(@ColorInt primaryShieldColor: Int) =
            apply { this.primaryShieldColor = primaryShieldColor }

        fun primaryCongestionLightColor(@ColorInt primaryCongestionLightColor: Int) =
            apply { this.primaryCongestionLightColor = primaryCongestionLightColor }

        fun primaryCongestionModerateColor(@ColorInt primaryCongestionModerateColor: Int) =
            apply { this.primaryCongestionModerateColor = primaryCongestionModerateColor }

        fun primaryCongestionHeavyColor(@ColorInt primaryCongestionHeavyColor: Int) =
            apply { this.primaryCongestionHeavyColor = primaryCongestionHeavyColor }

        fun primaryCongestionSevereColor(@ColorInt primaryCongestionSevereColor: Int) =
            apply { this.primaryCongestionSevereColor = primaryCongestionSevereColor }

        fun alternativeRouteVisible(alternativeRouteVisible: Boolean) =
            apply { this.alternativeRouteVisible = alternativeRouteVisible }

        fun alternativeRouteColor(@ColorInt alternativeRouteColor: Int) =
            apply { this.alternativeRouteColor = alternativeRouteColor }

        fun alternativeRouteScale(alternativeRouteScale: Double) =
            apply { this.alternativeRouteScale = alternativeRouteScale }

        fun alternativeShieldColor(@ColorInt alternativeShieldColor: Int) =
            apply { this.alternativeShieldColor = alternativeShieldColor }

        fun alternativeCongestionLightColor(@ColorInt alternativeCongestionLightColor: Int) =
            apply { this.alternativeCongestionLightColor = alternativeCongestionLightColor }

        fun alternativeCongestionModerateColor(@ColorInt alternativeCongestionModerateColor: Int) =
            apply { this.alternativeCongestionModerateColor = alternativeCongestionModerateColor }

        fun alternativeCongestionHeavyColor(@ColorInt alternativeCongestionHeavyColor: Int) =
            apply { this.alternativeCongestionHeavyColor = alternativeCongestionHeavyColor }

        fun alternativeCongestionSevereColor(@ColorInt alternativeCongestionSevereColor: Int) =
            apply { this.alternativeCongestionSevereColor = alternativeCongestionSevereColor }

        fun build() = RouteLineOptions(
            routeLayerPosition,
            roundedLineCap,
            primaryRouteIndex,
            primaryRouteVisible,
            primaryRouteColor,
            primaryRouteScale,
            primaryShieldColor,
            primaryCongestionLightColor,
            primaryCongestionModerateColor,
            primaryCongestionHeavyColor,
            primaryCongestionSevereColor,
            alternativeRouteVisible,
            alternativeRouteColor,
            alternativeRouteScale,
            alternativeShieldColor,
            alternativeCongestionLightColor,
            alternativeCongestionModerateColor,
            alternativeCongestionHeavyColor,
            alternativeCongestionSevereColor
        )
    }

    sealed class RouteLayerPosition(val referenceLayerId: String) {
        class Below(referenceLayerId: String) : RouteLayerPosition(referenceLayerId)
        class Above(referenceLayerId: String) : RouteLayerPosition(referenceLayerId)
    }
}

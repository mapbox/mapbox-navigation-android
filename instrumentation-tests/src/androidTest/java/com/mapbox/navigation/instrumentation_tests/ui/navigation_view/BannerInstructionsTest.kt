package com.mapbox.navigation.instrumentation_tests.ui.navigation_view

import android.text.SpannedString
import android.widget.TextView
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.idling.BannerInstructionsIdlingResource
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions
import kotlinx.android.synthetic.main.activity_basic_navigation_view.*
import org.junit.Assert.assertEquals
import org.junit.Test

class BannerInstructionsTest : SimpleNavigationViewTest() {

    @Test
    fun banner_text_distance_displayed_and_updated() {
        mockRoute.bannerInstructions.forEach {
            val distanceView = activity.navigationView.findViewById<TextView>(R.id.stepDistanceText)
            val formatter = MapboxDistanceFormatter.Builder(activity).build()
            val routeProgressObserver = object : RouteProgressObserver {
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    val text = formatter.formatDistance(
                        routeProgress
                            .currentLegProgress!!
                            .currentStepProgress!!
                            .distanceRemaining
                            .toDouble()
                    ).toString()
                    assertEquals(text, (distanceView.text as SpannedString).toString())
                }
            }
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)

            val bannerInstructionsIdlingResource = BannerInstructionsIdlingResource(
                mapboxNavigation,
                it
            )
            bannerInstructionsIdlingResource.register()
            BaristaVisibilityAssertions.assertContains(R.id.stepPrimaryText, it.primary().text())

            it.secondary()?.run {
                BaristaVisibilityAssertions.assertContains(R.id.stepSecondaryText, this.text())
            }?.run { BaristaVisibilityAssertions.assertNotDisplayed(R.id.stepSecondaryText) }

            it.sub()?.run {
                BaristaVisibilityAssertions.assertContains(R.id.subStepText, this.text())
            }?.run { BaristaVisibilityAssertions.assertNotDisplayed(R.id.subStepLayout) }

            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            bannerInstructionsIdlingResource.unregister()
        }
    }
}

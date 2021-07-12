package com.mapbox.navigation.qa_test_app.domain

import android.app.Activity
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingActivity
import com.mapbox.navigation.qa_test_app.view.MapboxRouteLineActivity
import com.mapbox.navigation.qa_test_app.view.RouteRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.RouteTrafficUpdateActivity
import com.mapbox.navigation.qa_test_app.view.util.RouteDrawingActivity

typealias LaunchActivityFun = (Activity) -> Unit

object TestActivitySuite {

    val testActivities = listOf(
        TestActivityDescription(
            "Alternative Route Selection",
            R.string.alternative_route_selection_description
        ) { activity ->
            activity.startActivity<AlternativeRouteActivity>()
        },
        TestActivityDescription(
            "Display Route Restrictions - Needs Update",
            R.string.route_restriction_activity_description
        ) { activity ->
            activity.startActivity<RouteRestrictionsActivity>()
        },
        TestActivityDescription(
            "Dynamic Traffic Update",
            R.string.dynamic_traffic_update_description
        ) { activity ->
            activity.startActivity<RouteTrafficUpdateActivity>()
        },
        TestActivityDescription(
            "Inactive Route Leg Styling",
            R.string.inactive_route_styling_description
        ) { activity ->
            activity.startActivity<InactiveRouteStylingActivity>()
        },
        TestActivityDescription(
            "Route Line dev. activity",
            R.string.routeline_activity
        ) { activity ->
            activity.startActivity<MapboxRouteLineActivity>()
        },
        TestActivityDescription(
            "Internal Route Drawing Utility",
            R.string.route_drawing_utility
        ) { activity ->
            activity.startActivity<RouteDrawingActivity>()
        }
    )
}

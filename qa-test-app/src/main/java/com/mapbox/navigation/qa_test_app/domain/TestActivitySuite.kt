package com.mapbox.navigation.qa_test_app.domain

import android.app.Activity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity
import com.mapbox.navigation.qa_test_app.view.AppLifecycleActivity
import com.mapbox.navigation.qa_test_app.view.FeedbackActivity
import com.mapbox.navigation.qa_test_app.view.IconsPreviewActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingWithRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.MapboxRouteLineActivity
import com.mapbox.navigation.qa_test_app.view.RouteRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.RouteTrafficUpdateActivity
import com.mapbox.navigation.qa_test_app.view.StatusActivity
import com.mapbox.navigation.qa_test_app.view.TrafficGradientActivity
import com.mapbox.navigation.qa_test_app.view.util.RouteDrawingActivity

typealias LaunchActivityFun = (Activity) -> Unit

object TestActivitySuite {

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    val testActivities = listOf(
        TestActivityDescription(
            "MapboxNavigation Lifecycle",
            R.string.mapbox_navigation_app_lifecycle_description
        ) { activity ->
            activity.startActivity<AppLifecycleActivity>()
        },
        TestActivityDescription(
            "Alternative Route Selection",
            R.string.alternative_route_selection_description
        ) { activity ->
            activity.startActivity<AlternativeRouteActivity>()
        },
        TestActivityDescription(
            "Display Route Restrictions",
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
            "Inactive Route Leg Styling With Restrictions",
            R.string.inactive_route_styling__with_restrictions_description
        ) { activity ->
            activity.startActivity<InactiveRouteStylingWithRestrictionsActivity>()
        },
        TestActivityDescription(
            "Traffic Gradient Styling",
            R.string.traffic_gradient_styling_description
        ) { activity ->
            activity.startActivity<TrafficGradientActivity>()
        },
        TestActivityDescription(
            "Feedback test activity",
            R.string.feedback_activity_description
        ) { activity ->
            activity.startActivity<FeedbackActivity>()
        },
        // add activities above this
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
        },
        TestActivityDescription(
            "Status View",
            R.string.status_activity_description
        ) { activity ->
            activity.startActivity<StatusActivity>()
        },
        TestActivityDescription(
            "Icons Preview",
            R.string.icons_preview_activity_description
        ) { activity ->
            activity.startActivity<IconsPreviewActivity>()
        },
    )
}

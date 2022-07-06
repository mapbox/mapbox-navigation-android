package com.mapbox.navigation.qa_test_app.domain

import android.app.Activity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity
import com.mapbox.navigation.qa_test_app.view.AppLifecycleActivity
import com.mapbox.navigation.qa_test_app.view.ComponentsActivity
import com.mapbox.navigation.qa_test_app.view.ComponentsAltActivity
import com.mapbox.navigation.qa_test_app.view.CustomAlternativeRouteColoringActivity
import com.mapbox.navigation.qa_test_app.view.DropInButtonsActivity
import com.mapbox.navigation.qa_test_app.view.FeedbackActivity
import com.mapbox.navigation.qa_test_app.view.IconsPreviewActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingWithRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewCustomizedActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewFragmentActivity
import com.mapbox.navigation.qa_test_app.view.MapboxRouteLineActivity
import com.mapbox.navigation.qa_test_app.view.NavigationViewFragmentLifecycleActivity
import com.mapbox.navigation.qa_test_app.view.RestStopActivity
import com.mapbox.navigation.qa_test_app.view.RouteLineFeaturesActivity
import com.mapbox.navigation.qa_test_app.view.RouteNumericTrafficUpdateActivity
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
            "Components install via MapboxNavigationApp",
            R.string.experimental_components_install,
        ) { activity ->
            activity.startActivity<ComponentsActivity>()
        },
        TestActivityDescription(
            "Components install via MapboxNavigation",
            R.string.experimental_alt_components_install,
        ) { activity ->
            activity.startActivity<ComponentsAltActivity>()
        },
        TestActivityDescription(
            "MapboxNavigation Lifecycle",
            R.string.mapbox_navigation_app_lifecycle_description,
        ) { activity ->
            activity.startActivity<AppLifecycleActivity>()
        },
        TestActivityDescription(
            "Alternative Route Selection",
            R.string.alternative_route_selection_description,
        ) { activity ->
            activity.startActivity<AlternativeRouteActivity>()
        },
        TestActivityDescription(
            "Custom Alt. Route Colors",
            R.string.custom_alternative_route_color_description,
        ) { activity ->
            activity.startActivity<CustomAlternativeRouteColoringActivity>()
        },
        TestActivityDescription(
            "Display Route Restrictions",
            R.string.route_restriction_activity_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<RouteRestrictionsActivity>()
        },
        TestActivityDescription(
            "Log rest stops",
            R.string.rest_stop_activity_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<RestStopActivity>()
        },
        TestActivityDescription(
            "Dynamic Traffic Update",
            R.string.dynamic_traffic_update_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<RouteTrafficUpdateActivity>()
        },
        TestActivityDescription(
            "Dynamic Numeric Traffic Update",
            R.string.dynamic_numeric_traffic_update_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<RouteNumericTrafficUpdateActivity>()
        },
        TestActivityDescription(
            "Inactive Route Leg Styling",
            R.string.inactive_route_styling_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<InactiveRouteStylingActivity>()
        },
        TestActivityDescription(
            "Inactive Route Leg Styling With Restrictions",
            R.string.inactive_route_styling__with_restrictions_description,
            launchAfterPermissionResult = false,
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
            "Basic Route Line Features",
            R.string.basic_route_line_features_description
        ) { activity ->
            activity.startActivity<RouteLineFeaturesActivity>()
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
        TestActivityDescription(
            "Default NavigationView",
            R.string.navigation_view_description,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<MapboxNavigationViewActivity>() },
        TestActivityDescription(
            "Customized NavigationView",
            R.string.navigation_view_customized_description,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<MapboxNavigationViewCustomizedActivity>() },
        TestActivityDescription(
            "Fullscreen NavigationView in a Fragment",
            R.string.navigation_view_fragment_description,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<MapboxNavigationViewFragmentActivity>() },
        TestActivityDescription(
            "NavigationView lifecycle test with Fragments",
            R.string.navigation_view_fragment_lifecycle_description,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<NavigationViewFragmentLifecycleActivity>() },
        TestActivityDescription(
            "Drop In Buttons",
            R.string.drop_in_buttons_activity_description
        ) { activity ->
            activity.startActivity<DropInButtonsActivity>()
        },
    )
}

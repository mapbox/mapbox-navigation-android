package com.mapbox.navigation.qa_test_app.domain

import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.ActiveLegAboveInactiveLegsActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity
import com.mapbox.navigation.qa_test_app.view.AppLifecycleActivity
import com.mapbox.navigation.qa_test_app.view.CustomAlternativeRouteColoringActivity
import com.mapbox.navigation.qa_test_app.view.DropInButtonsActivity
import com.mapbox.navigation.qa_test_app.view.FeedbackActivity
import com.mapbox.navigation.qa_test_app.view.IconsPreviewActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingActivity
import com.mapbox.navigation.qa_test_app.view.InactiveRouteStylingWithRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewFragmentActivity
import com.mapbox.navigation.qa_test_app.view.MapboxNavigationViewOfflineOnlineRouteSwitchActivity
import com.mapbox.navigation.qa_test_app.view.MapboxRouteLineActivity
import com.mapbox.navigation.qa_test_app.view.NavigationViewFragmentLifecycleActivity
import com.mapbox.navigation.qa_test_app.view.RoadObjectsActivity
import com.mapbox.navigation.qa_test_app.view.RouteLineFeaturesActivity
import com.mapbox.navigation.qa_test_app.view.RouteLineScalingActivity
import com.mapbox.navigation.qa_test_app.view.RouteNumericTrafficUpdateActivity
import com.mapbox.navigation.qa_test_app.view.RouteRefreshActivity
import com.mapbox.navigation.qa_test_app.view.RouteRestrictionsActivity
import com.mapbox.navigation.qa_test_app.view.RouteTrafficUpdateActivity
import com.mapbox.navigation.qa_test_app.view.RoutesPreviewActivity
import com.mapbox.navigation.qa_test_app.view.SpeedInfoActivity
import com.mapbox.navigation.qa_test_app.view.StatusActivity
import com.mapbox.navigation.qa_test_app.view.TrafficGradientActivity
import com.mapbox.navigation.qa_test_app.view.TripOverviewActivity
import com.mapbox.navigation.qa_test_app.view.UpcomingRoadObjectsActivity
import com.mapbox.navigation.qa_test_app.view.componentinstaller.ComponentsActivity
import com.mapbox.navigation.qa_test_app.view.componentinstaller.ComponentsAltActivity
import com.mapbox.navigation.qa_test_app.view.componentinstaller.RestAreaActivity
import com.mapbox.navigation.qa_test_app.view.customnavview.MapboxNavigationViewCustomizedActivity
import com.mapbox.navigation.qa_test_app.view.main.SelectDestinationDialogFragment
import com.mapbox.navigation.qa_test_app.view.util.RouteDrawingActivity

typealias LaunchActivityFun = (AppCompatActivity) -> Unit

data class Destination(val name: String, val point: Point)

object TestActivitySuite {

    private val testDestinations = listOf(
        Destination("Newmarket: A&B office", Point.fromLngLat(-79.4443, 44.0620)),
        Destination("Toronto: Lume Kitchen and Lounge", Point.fromLngLat(-79.4843, 43.6244))
    )

    val testActivities = listOf(
        TestActivityDescription(
            "Components install via MapboxNavigationApp",
            R.string.experimental_components_install,
            category = CATEGORY_COMPONENTS
        ) { activity ->
            activity.startActivity<ComponentsActivity>()
        },
        TestActivityDescription(
            "Components install via MapboxNavigation",
            R.string.experimental_alt_components_install,
            category = CATEGORY_COMPONENTS
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
            "Display Road Objects",
            R.string.road_object_activity_description,
            launchAfterPermissionResult = false,
        ) { activity ->
            activity.startActivity<RoadObjectsActivity>()
        },
        TestActivityDescription(
            "List Upcoming Road Objects",
            R.string.road_object_activity_description,
        ) { activity ->
            activity.startActivity<UpcomingRoadObjectsActivity>()
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
            "Custom Scaling Applied",
            R.string.route_line_scaling_description
        ) { activity ->
            activity.startActivity<RouteLineScalingActivity>()
        },
        TestActivityDescription(
            "Active route leg above other legs.",
            R.string.route_line_active_leg_above_others_description
        ) { activity ->
            activity.startActivity<ActiveLegAboveInactiveLegsActivity>()
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
            "Visualize Trip Overview",
            R.string.trip_overview_activity_description
        ) { activity ->
            activity.startActivity<TripOverviewActivity>()
        },
        TestActivityDescription(
            "Default NavigationView",
            R.string.navigation_view_description,
            category = CATEGORY_DROP_IN,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<MapboxNavigationViewActivity>() },
        TestActivityDescription(
            "Customized NavigationView",
            R.string.navigation_view_customized_description,
            category = CATEGORY_DROP_IN,
            launchAfterPermissionResult = false
        ) { activity -> activity.startActivity<MapboxNavigationViewCustomizedActivity>() },
        TestActivityDescription(
            "Navigate to point with NavigationView",
            R.string.navigation_view_description,
            category = CATEGORY_DROP_IN,
            launchAfterPermissionResult = false
        ) { activity ->
            SelectDestinationDialogFragment(testDestinations) { dest, startReplay ->
                MapboxNavigationViewActivity.startActivity(activity, dest.point, startReplay)
            }.show(activity.supportFragmentManager, SelectDestinationDialogFragment.TAG)
        },
        TestActivityDescription(
            "Fullscreen NavigationView in a Fragment",
            R.string.navigation_view_fragment_description,
            launchAfterPermissionResult = false,
            category = CATEGORY_DROP_IN,
        ) { activity -> activity.startActivity<MapboxNavigationViewFragmentActivity>() },
        TestActivityDescription(
            "NavigationView lifecycle test with Fragments",
            R.string.navigation_view_fragment_lifecycle_description,
            launchAfterPermissionResult = false,
            category = CATEGORY_DROP_IN,
        ) { activity -> activity.startActivity<NavigationViewFragmentLifecycleActivity>() },
        TestActivityDescription(
            "Drop In Buttons",
            R.string.drop_in_buttons_activity_description,
            category = CATEGORY_DROP_IN
        ) { activity ->
            activity.startActivity<DropInButtonsActivity>()
        },
        TestActivityDescription(
            "Route preview",
            R.string.drop_in_buttons_activity_description
        ) { activity ->
            activity.startActivity<RoutesPreviewActivity>()
        },
        TestActivityDescription(
            "Speed Info",
            R.string.speed_info_activity_description
        ) { activity ->
            activity.startActivity<SpeedInfoActivity>()
        },
        TestActivityDescription(
            "Rest Area Example",
            R.string.rest_area_activity_description,
            category = CATEGORY_COMPONENTS
        ) { activity ->
            activity.startActivity<RestAreaActivity>()
        },
        TestActivityDescription(
            "Route Refresh Example",
            R.string.description_route_refresh,
        ) { activity ->
            activity.startActivity<RouteRefreshActivity>()
        },
        TestActivityDescription(
            "Switch from offline to online route",
            R.string.navigation_view_offline_online_route_switch,
            category = CATEGORY_DROP_IN,
            launchAfterPermissionResult = false
        ) { activity ->
            MapboxNavigationViewOfflineOnlineRouteSwitchActivity.startActivity(activity)
        }
    )

    fun getTestActivities(category: String): List<TestActivityDescription> {
        return if (category == CATEGORY_NONE) {
            testActivities
        } else {
            testActivities.filter { it.category == category }
        }
    }
}

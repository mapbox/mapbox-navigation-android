package com.mapbox.navigation.qa_test_app.domain

import android.app.Activity
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity
import com.mapbox.navigation.qa_test_app.view.ForceRecreatingNavigatorInstanceActivity

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
            "Recreating Navigator",
            R.string.force_navigator_recreating_description
        ) { activity ->
            activity.startActivity<ForceRecreatingNavigatorInstanceActivity>()
        }
    )
}

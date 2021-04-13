package com.mapbox.navigation.qa_test_app.domain

import android.app.Activity
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.utils.startActivity
import com.mapbox.navigation.qa_test_app.view.AlternativeRouteActivity

typealias LaunchActivityFun = (Activity) -> Unit
object TestActivitySuite {

    val testActivities = listOf(
        TestActivityDescription("Alternative Route Selection", R.string.alternative_route_selection_description) { activity ->
            activity.startActivity<AlternativeRouteActivity>()
        }
    )

}

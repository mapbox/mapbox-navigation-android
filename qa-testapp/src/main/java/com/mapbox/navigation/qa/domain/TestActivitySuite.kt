package com.mapbox.navigation.qa.domain

import android.app.Activity
import com.mapbox.navigation.qa.R
import com.mapbox.navigation.qa.domain.model.TestActivityCategory
import com.mapbox.navigation.qa.domain.model.TestActivityDescription
import com.mapbox.navigation.qa.view.SampleTestActivity
import com.mapbox.navigation.qa.utils.startActivity

typealias LaunchActivityFun = (Activity) -> Unit
object TestActivitySuite {

    val categories = listOf<TestActivityCategory>(
        TestActivityCategory("Category A", listOf(
            TestActivityDescription("Sample Test Activity", R.string.sample_test_activity_description) { activity ->
                activity.startActivity<SampleTestActivity>()
            }
        )),
        TestActivityCategory("Category B", listOf()),
    )

    fun getCategoryActivityDescriptions(categoryName: String): List<TestActivityDescription> =
        categories.first { it.label == categoryName }.testActivityDescriptions
}

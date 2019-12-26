package com.mapbox.navigation.testing.ui.rules

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.mapbox.navigation.testing.ui.R
import org.junit.Rule


open class BaseTestRule<A : AppCompatActivity>(activityClass: Class<A>) : TestCase() {

    @get:Rule
    val activityRule = ActivityTestRule(activityClass, true, true)

    @get:Rule
    val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    val activity: A
        get() = activityRule.activity

    val appName: String by lazy { activity.getString(R.string.app_name) }
}

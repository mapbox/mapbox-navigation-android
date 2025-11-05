package com.mapbox.navigation.testing.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import org.junit.Before
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

abstract class BeforeLaunchRule : ExternalResource() {

    protected abstract fun executeBeforeLaunch()

    final override fun before() {
        executeBeforeLaunch()
    }

    override fun after() {
        // no-op
    }
}

abstract class BaseActionBeforeLaunchTest<A : AppCompatActivity>(
    activityClass: Class<A>
) : BaseCoreNoCleanUpTest() {

    abstract fun executeBeforeLaunch()

    private val beforeLaunchRule = object : ExternalResource() {
        override fun before() {
            executeBeforeLaunch()
        }

        override fun after() {
            // no-op
        }
    }
    val activityRule = ActivityScenarioRule(activityClass)

    @get:Rule
    val launchActivityRuleChain: RuleChain = RuleChain
        .outerRule(beforeLaunchRule)
        .around(activityRule)

    // Clear all app's SharedPreferences
    // This allows the test to run in isolation, without any cached resources.
    @get:Rule
    val clearPreferencesRule = ClearPreferencesRule()

    // Delete all tables from all the app's SQLite Databases.
    // Including the database that store Map SDK resources.
    // This allows the test to run in isolation, without any cached resources.
    @get:Rule
    val clearDatabaseRule = ClearDatabaseRule()

    // Delete all files in getFilesDir() and getCacheDir()
    // This allows the test to run in isolation, without any cached resources.
    @get:Rule
    val clearFilesRule = ClearFilesRule()

    protected val uiDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private lateinit var _activity: A
    val activity: A get() = _activity

    @Before
    fun initializeActivity() {
        activityRule.scenario.onActivity { _activity = it }
    }
}

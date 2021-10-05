package com.mapbox.navigation.testing.ui

import android.Manifest
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import com.schibsted.spain.barista.rule.cleardata.ClearDatabaseRule
import com.schibsted.spain.barista.rule.cleardata.ClearFilesRule
import com.schibsted.spain.barista.rule.cleardata.ClearPreferencesRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule

abstract class BaseTest<A : AppCompatActivity>(activityClass: Class<A>) {

    companion object {
        @ClassRule
        @JvmField
        val mockLocationUpdatesRule = MockLocationUpdatesRule()
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @get:Rule
    val activityRule = ActivityTestRule(activityClass)

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

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

    protected val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    val activity: A
        get() = activityRule.activity

    @Before
    fun runSetupMockLocation() {
        mockLocationUpdatesRule.pushLocationUpdate(setupMockLocation())
    }

    // The MockLocationUpdatesRule will uses the system's GPS provider.
    // Considering that any device, at any location, can run a test;
    // the initial location is ambiguous if it is not specified.
    //
    // It is required to specify real location in the tests.
    // Do not return Location(0,0) unless the test is explicitly testing initialization.
    abstract fun setupMockLocation(): Location
}

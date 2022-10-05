package com.mapbox.navigation.testing.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.DDGrantPermissionRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.rule.cleardata.ClearDatabaseRule
import com.adevinta.android.barista.rule.cleardata.ClearFilesRule
import com.adevinta.android.barista.rule.cleardata.ClearPreferencesRule
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule

abstract class BaseTest<A : AppCompatActivity>(activityClass: Class<A>) {

    companion object {
        @ClassRule
        @JvmField
        val mockLocationUpdatesRule = MockLocationUpdatesRule()
    }

    private val permissionsToGrant by lazy {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule = DDGrantPermissionRule().also {
        it.permissionGranter.addPermissions(*permissionsToGrant.toTypedArray())
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(activityClass)

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

    protected val uiDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private lateinit var _activity: A
    val activity: A get() = _activity

    @RequiresApi(Build.VERSION_CODES.P)
    @Before
    fun initializeActivity() {
        activityRule.scenario.onActivity {
            _activity = it
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            permissionsToGrant.forEach { permission ->
                uiAutomation.grantRuntimePermission("com.mapbox.navigation.instrumentation_tests", permission)
                var attempts = 0
                while (attempts < 10) {
                    if (ContextCompat.checkSelfPermission(it, permission) != PackageManager.PERMISSION_GRANTED) {
                        Thread.sleep(1000)
                    }
                    attempts++
                }

            }
        }
    }

//    @Before
//    fun runSetupMockLocation() {
//        mockLocationUpdatesRule.pushLocationUpdate(setupMockLocation())
//    }

    // The MockLocationUpdatesRule will uses the system's GPS provider.
    // Considering that any device, at any location, can run a test;
    // the initial location is ambiguous if it is not specified.
    //
    // It is required to specify real location in the tests.
    // Do not return Location(0,0) unless the test is explicitly testing initialization.
    open fun setupMockLocation(): Location = Location("gps")
}

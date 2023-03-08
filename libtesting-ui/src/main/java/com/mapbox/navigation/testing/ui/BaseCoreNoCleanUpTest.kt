package com.mapbox.navigation.testing.ui

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain

abstract class BaseCoreNoCleanUpTest {

    private val permissionsToGrant by lazy {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }

    private val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        *permissionsToGrant.toTypedArray()
    )

    val mockLocationUpdatesRule = MockLocationUpdatesRule()

    // We should first grant permissions, then set up location
    @get:Rule
    val permissionsThenLocationChain = RuleChain
        .outerRule(permissionsRule)
        .around(mockLocationUpdatesRule)

    @get:Rule
    val mockWebServerRule = MockWebServerRule()

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

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

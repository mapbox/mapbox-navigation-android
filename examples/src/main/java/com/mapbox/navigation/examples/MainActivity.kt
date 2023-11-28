package com.mapbox.navigation.examples

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel
import com.mapbox.navigation.examples.core.IndependentRouteGenerationActivity
import com.mapbox.navigation.examples.core.MapboxBuildingHighlightActivity
import com.mapbox.navigation.examples.core.MapboxCustomStyleActivity
import com.mapbox.navigation.examples.core.MapboxJunctionActivity
import com.mapbox.navigation.examples.core.MapboxManeuverActivity
import com.mapbox.navigation.examples.core.MapboxMultipleArrowActivity
import com.mapbox.navigation.examples.core.MapboxNavigationActivity
import com.mapbox.navigation.examples.core.MapboxRouteLineAndArrowActivity
import com.mapbox.navigation.examples.core.MapboxSignboardActivity
import com.mapbox.navigation.examples.core.MapboxTripProgressActivity
import com.mapbox.navigation.examples.core.MapboxVoiceActivity
import com.mapbox.navigation.examples.core.MultiLegRouteExampleActivity
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.ReplayHistoryActivity
import com.mapbox.navigation.examples.core.camera.MapboxCameraAnimationsActivity
import com.mapbox.navigation.examples.core.databinding.LayoutActivityMainBinding
import com.mapbox.navigation.examples.util.LocationPermissionsHelper
import com.mapbox.navigation.examples.util.LocationPermissionsHelper.Companion.areLocationPermissionsGranted
import com.mapbox.navigation.examples.util.RouteDrawingActivity

class MainActivity : AppCompatActivity(), PermissionsListener {

    private val locationPermissionsHelper = LocationPermissionsHelper(this)
    private lateinit var binding: LayoutActivityMainBinding
    private lateinit var adapter: ExamplesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
        binding = LayoutActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sampleItemList = buildSampleList()
        adapter = ExamplesAdapter(this) {
            startActivity(Intent(this@MainActivity, sampleItemList[it].activity))
        }
        binding.coreRecycler.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = this@MainActivity.adapter
        }
        adapter.addSampleItems(sampleItemList)

        if (areLocationPermissionsGranted(this)) {
            requestOptionalPermissions()
        } else {
            locationPermissionsHelper.requestLocationPermissions(this)
        }
    }

    private fun buildSampleList(): List<SampleItem> {
        return listOf(
            SampleItem(
                getString(R.string.title_navigation),
                getString(R.string.description_navigation),
                MapboxNavigationActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_route_line),
                getString(R.string.description_route_line),
                MapboxRouteLineAndArrowActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_camera),
                getString(R.string.description_camera),
                MapboxCameraAnimationsActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_signboard),
                getString(R.string.description_signboard),
                MapboxSignboardActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_junction),
                getString(R.string.description_junction),
                MapboxJunctionActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_trip_progress),
                getString(R.string.description_trip_progress),
                MapboxTripProgressActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_maneuver),
                getString(R.string.description_maneuver),
                MapboxManeuverActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_voice),
                getString(R.string.description_voice),
                MapboxVoiceActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_custom_style),
                getString(R.string.description_custom_style),
                MapboxCustomStyleActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_replay),
                getString(R.string.description_replay),
                ReplayHistoryActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_multiple_arrows),
                getString(R.string.description_multiple_arrows),
                MapboxMultipleArrowActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_independent_route),
                getString(R.string.description_independent_route),
                IndependentRouteGenerationActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_building_highlight),
                getString(R.string.description_building_highlight),
                MapboxBuildingHighlightActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_multileg_route),
                getString(R.string.description_multileg_route),
                MultiLegRouteExampleActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_draw_utility),
                getString(R.string.description_draw_utility),
                RouteDrawingActivity::class.java
            ),
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(
            this,
            "This app needs location permission in order to show its functionality.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestOptionalPermissions()
        } else {
            Toast.makeText(
                this,
                "You didn't grant location permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestOptionalPermissions() {
        // starting from Android R leak canary writes to Download storage without the permission
        val permissionsToRequest = mutableListOf<String>()
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(permission.WRITE_EXTERNAL_STORAGE)
        }
        /*if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(permission.POST_NOTIFICATIONS)
        }*/
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                10
            )
        }
    }
}

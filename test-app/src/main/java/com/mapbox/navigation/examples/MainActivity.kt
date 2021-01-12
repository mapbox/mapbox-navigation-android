package com.mapbox.navigation.examples

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.navigation.examples.core.MapboxManeuverActivity
import com.mapbox.navigation.examples.core.MapboxRouteLineApiExampleActivity
import com.mapbox.navigation.examples.core.MapboxSignboardActivity
import com.mapbox.navigation.examples.core.MapboxSnapshotActivity
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.SlackLineActivity
import com.mapbox.navigation.examples.core.TripProgressActivity
import com.mapbox.navigation.examples.core.camera.CameraAnimationsActivity
import com.mapbox.navigation.examples.util.LocationPermissionsHelper
import com.mapbox.navigation.examples.util.LocationPermissionsHelper.Companion.areLocationPermissionsGranted
import kotlinx.android.synthetic.main.main_activity_layout.*

class MainActivity : AppCompatActivity(), PermissionsListener {

    private val permissionsHelper = LocationPermissionsHelper(this)
    private lateinit var adapter: ExamplesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        val sampleItemList = buildSampleList()
        adapter = ExamplesAdapter(this) {
            startActivity(Intent(this@MainActivity, sampleItemList[it].activity))
        }
        coreRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        coreRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        coreRecycler.adapter = adapter
        adapter.addSampleItems(sampleItemList)

        if (areLocationPermissionsGranted(this)) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionsHelper.requestLocationPermissions(this)
        }
    }

    private fun buildSampleList(): List<SampleItem> {
        return listOf(
            SampleItem(
                getString(R.string.title_route_api),
                getString(R.string.description_routeline_api),
                MapboxRouteLineApiExampleActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_slackline),
                getString(R.string.slackline_description),
                SlackLineActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_navigation_camera),
                getString(R.string.description_navigation_camera),
                CameraAnimationsActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_snapshotter),
                getString(R.string.description_snapshotter),
                MapboxSnapshotActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_signboard),
                getString(R.string.description_signboard),
                MapboxSignboardActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_trip_progress),
                getString(R.string.description_trip_progress),
                TripProgressActivity::class.java
            ),
            SampleItem(
                getString(R.string.title_maneuver),
                getString(R.string.description_maneuver),
                MapboxManeuverActivity::class.java
            )
        )
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this,
            "This app needs location and storage permissions in order to show its functionality.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(
                this,
                "You didn't grant location permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded: MutableList<String> = ArrayList()
        if (
            ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                10
            )
        } else {
            //
        }
    }
}

package com.mapbox.navigation.examples.mincompile

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
import com.mapbox.navigation.examples.mincompile.databinding.LayoutActivityMainBinding
import com.mapbox.navigation.examples.mincompile.LocationPermissionsHelper.Companion.areLocationPermissionsGranted

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
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                10
            )
        }
    }
}

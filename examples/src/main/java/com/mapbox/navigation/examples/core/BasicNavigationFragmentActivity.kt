package com.mapbox.navigation.examples.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.examples.R
import java.util.ArrayList
import kotlinx.android.synthetic.main.activity_basic_navigation_fragment.*

/**
 * This activity shows how to create a turn-by-turn navigation from a Fragment.
 */
class BasicNavigationFragmentActivity : AppCompatActivity(), PermissionsListener {
    private val permissionsManager = PermissionsManager(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_fragment)
        when (PermissionsManager.areLocationPermissionsGranted(this)) {
            true -> requestPermissionIfNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast
                .makeText(
                        this,
                        "This app needs location and storage permissions" + "in order to show its functionality.",
                        Toast.LENGTH_LONG
                ).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            when (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                true -> {
                    basicNavigationFragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
                }
                else -> {
                    Toast.makeText(this, "You didn't grant storage permissions.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 10)
        } else {
            basicNavigationFragment.onRequestPermissionsResult(0, emptyArray(), intArrayOf())
        }
    }
}

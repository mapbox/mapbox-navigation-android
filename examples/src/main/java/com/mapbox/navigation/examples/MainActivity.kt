package com.mapbox.navigation.examples

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.examples.settings.NavigationSettingsActivity
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import java.util.ArrayList
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PermissionsListener {

    private val permissionsManager = PermissionsManager(this)
    private val CHANGE_SETTING_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        when (PermissionsManager.areLocationPermissionsGranted(this)) {
            true -> requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE)
            else -> permissionsManager.requestLocationPermissions(this)
        }

        settingsFab.setOnClickListener {
            startActivityForResult(
                Intent(this@MainActivity, NavigationSettingsActivity::class.java),
                CHANGE_SETTING_REQUEST_CODE
            )
        }

        cardCore.setOnClickListener {
            startActivity(Intent(this@MainActivity, CoreActivity::class.java))
        }

        cardUI.setOnClickListener {
            startActivity(Intent(this@MainActivity, UIActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHANGE_SETTING_REQUEST_CODE) {
            updateNavNativeHistoryCollection()
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

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0) {
            permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {
            when (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                true -> {
                    cardCore.isClickable = true
                }
                else -> {
                    cardCore.isClickable = false
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
        }
    }

    private fun updateNavNativeHistoryCollection() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        MapboxNativeNavigatorImpl.toggleHistory(
            prefs.getBoolean(getString(R.string.nav_native_history_collect_key), false)
        )
    }
}

package com.mapbox.navigation.qa_test_app.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ActivityMainBinding
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription
import com.mapbox.navigation.qa_test_app.domain.TestActivitySuite
import com.mapbox.navigation.qa_test_app.utils.LOCATION_PERMISSIONS_REQUEST_CODE
import com.mapbox.navigation.qa_test_app.utils.LocationPermissionsHelper
import com.mapbox.navigation.qa_test_app.view.adapters.ActivitiesListAdaptersSupport
import com.mapbox.navigation.qa_test_app.view.adapters.GenericListAdapter
import com.mapbox.navigation.qa_test_app.view.adapters.GenericListAdapterItemSelectedFun
import java.util.ArrayList

/**
 * To add additional activities see [TestActivitySuite]. Alteration of this class shouldn't be
 * necessary.
 */
class MainActivity : AppCompatActivity(), PermissionsListener {

    private val permissionsHelper = LocationPermissionsHelper(this)
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val layoutManager = LinearLayoutManager(this)
        binding.activitiesList.layoutManager = LinearLayoutManager(this)
        binding.activitiesList.adapter = GenericListAdapter(
            ActivitiesListAdaptersSupport.activitiesListOnBindViewHolderFun,
            ActivitiesListAdaptersSupport.viewHolderFactory,
            activitySelectedDelegate,
            auxViewClickMap = mapOf(Pair(R.id.infoLabel, infoIconClickListenerFun))
        )
        binding.activitiesList.addItemDecoration(
            DividerItemDecoration(this, layoutManager.orientation)
        )

        when (LocationPermissionsHelper.areLocationPermissionsGranted(this)) {
            true -> requestPermissionIfNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else -> permissionsHelper.requestLocationPermissions(this)
        }
    }

    override fun onStart() {
        super.onStart()
        (binding.activitiesList.adapter as GenericListAdapter<TestActivityDescription, *>).swap(
            TestActivitySuite.testActivities
        )
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast
            .makeText(
                this,
                "This app needs location and storage permissions" +
                    "in order to show its functionality.",
                Toast.LENGTH_LONG
            ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, "You didn't grant location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } else {

            when (
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                true -> {
                    binding.activitiesList.isClickable = true
                }
                else -> {
                    binding.activitiesList.isClickable = false
                    Toast.makeText(
                        this,
                        "You didn't grant storage or location permissions.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 10)
        }
    }

    private val activitySelectedDelegate:
        GenericListAdapterItemSelectedFun<TestActivityDescription> = { positionAndElement ->
            positionAndElement.second.launchActivityFun(this)
        }

    private val infoIconClickListenerFun:
        GenericListAdapterItemSelectedFun<TestActivityDescription> = { positionAndElement ->
            AlertDialog.Builder(this)
                .setMessage(positionAndElement.second.fullDescriptionResource)
                .setTitle("Test Description")
                .setPositiveButton("Ok") { dlg, _ ->
                    dlg.dismiss()
                }.show()
        }
}

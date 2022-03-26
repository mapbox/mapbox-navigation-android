package com.mapbox.navigation.qa_test_app.view

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.ActivityMainBinding
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription
import com.mapbox.navigation.qa_test_app.domain.TestActivitySuite
import com.mapbox.navigation.qa_test_app.utils.PermissionsHelper
import com.mapbox.navigation.qa_test_app.utils.PermissionsState
import com.mapbox.navigation.qa_test_app.view.adapters.ActivitiesListAdaptersSupport
import com.mapbox.navigation.qa_test_app.view.adapters.GenericListAdapter
import com.mapbox.navigation.qa_test_app.view.adapters.GenericListAdapterItemSelectedFun
import kotlinx.coroutines.launch

/**
 * To add additional activities see [TestActivitySuite]. Alteration of this class shouldn't be
 * necessary.
 */
class MainActivity : AppCompatActivity() {

    private val permissionsHelper = PermissionsHelper()
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
    }

    override fun onStart() {
        super.onStart()
        (binding.activitiesList.adapter as GenericListAdapter<TestActivityDescription, *>).swap(
            TestActivitySuite.testActivities
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun launchAfterPermissionResult(element: TestActivityDescription) {
        lifecycleScope.launch {
            val ready = permissionsHelper.checkAndRequestPermissions(
                this@MainActivity,
                DEFAULT_PERMISSIONS
            )
            showToastForDeniedPermissions(ready)
            element.launchActivityFun(this@MainActivity)
        }
    }

    private fun showToastForDeniedPermissions(ready: PermissionsState.Ready) {
        val permissionsDenied = ready.results.filter { !it.isGranted }.map { it.permission }
        if (permissionsDenied.isNotEmpty()) {
            val toastMessage = when {
                permissionsDenied.containsAll(DEFAULT_PERMISSIONS) ->
                    "You didn't grant location or storage permissions."
                permissionsDenied.contains(Manifest.permission.ACCESS_FINE_LOCATION) ->
                    "You didn't grant location permissions."
                permissionsDenied.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) ->
                    "You didn't grant storage permissions."
                else -> "Permissions denied: ${permissionsDenied.joinToString()}"
            }
            Toast.makeText(this@MainActivity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    private val activitySelectedDelegate:
        GenericListAdapterItemSelectedFun<TestActivityDescription> = { _, element ->
            if (element.launchAfterPermissionResult) {
                launchAfterPermissionResult(element)
            } else {
                element.launchActivityFun(this)
            }
        }

    private val infoIconClickListenerFun:
        GenericListAdapterItemSelectedFun<TestActivityDescription> = { _, element ->
            AlertDialog.Builder(this)
                .setMessage(element.fullDescriptionResource)
                .setTitle("Test Description")
                .setPositiveButton("Ok") { dlg, _ ->
                    dlg.dismiss()
                }.show()
        }

    companion object {
        private val DEFAULT_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
    }
}

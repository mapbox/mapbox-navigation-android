package com.mapbox.navigation.qa_test_app.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

private const val PERMISSIONS_REQUEST_CODE = 1612

data class PermissionResult(
    val permission: String,
    val isGranted: Boolean
)

sealed class PermissionsState {
    object Idle : PermissionsState()
    data class Requesting(
        val permissions: List<String>,
        val permissionsToExplain: List<String>,
    ) : PermissionsState()
    data class Ready(
        val results: List<PermissionResult>
    ) : PermissionsState()
}

/**
 * Helper class for accepting permissions with suspending functions.
 */
class PermissionsHelper {

    private val state = MutableStateFlow<PermissionsState>(PermissionsState.Idle)

    /**
     * This method will suspend until the results have been captured from
     * [onRequestPermissionsResult]. It will return immediately if all the requested permissions
     * have been granted.
     */
    suspend fun checkAndRequestPermissions(
        activity: Activity,
        permissions: List<String>
    ): PermissionsState.Ready {
        val checkedResults = permissions.map { permission ->
            val code = ContextCompat.checkSelfPermission(activity, permission)
            PermissionResult(permission, code == PackageManager.PERMISSION_GRANTED)
        }
        val permissionsNeedGranting = checkedResults
            .filter { !it.isGranted }
            .map { it.permission }
        if (checkedResults.all { it.isGranted } || permissionsNeedGranting.isEmpty()) {
            state.value = PermissionsState.Ready(checkedResults)
        } else if (areRuntimePermissionsRequired()) {
            requestPermissions(activity, permissionsNeedGranting)
        }
        return state.filterIsInstance<PermissionsState.Ready>().first()
    }

    private fun areRuntimePermissionsRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun requestPermissions(activity: Activity, permissions: List<String>) {
        val permissionsToExplain = mutableListOf<String>()
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                permissionsToExplain.add(permission)
            }
        }
        state.value = PermissionsState.Requesting(
            permissions = permissions,
            permissionsToExplain = permissionsToExplain,
        )
        ActivityCompat.requestPermissions(
            activity,
            permissions.toTypedArray(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    /**
     * You should call this method from your activity onRequestPermissionsResult.
     *
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     * PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val results = permissions.toList().zip(grantResults.toList()).map {
                PermissionResult(it.first, it.second == PackageManager.PERMISSION_GRANTED)
            }
            state.value = PermissionsState.Ready(results)
        }
    }
}

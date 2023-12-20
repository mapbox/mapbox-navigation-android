package com.mapbox.navigation.examples.mincompile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsListener
import java.util.ArrayList

private const val COARSE_LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
private const val FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
private const val BACKGROUND_LOCATION_PERMISSION = "android.permission.ACCESS_BACKGROUND_LOCATION"
const val LOCATION_PERMISSIONS_REQUEST_CODE = 1612

/**
 * It's a temporary replacement for PermissionsManager from com.mapbox.android.core.permissions
 * See https://github.com/mapbox/mapbox-events-android/issues/490
 */
class LocationPermissionsHelper(private val listener: PermissionsListener?) {

    fun requestLocationPermissions(activity: Activity) {
        // Request fine location permissions by default
        requestLocationPermissions(activity, true, true)
    }

    // suppressed to have the same API as PermissionsManager from com.mapbox.android.core.permissions
    @Suppress("SameParameterValue")
    private fun requestLocationPermissions(
        activity: Activity,
        requestFineLocation: Boolean,
        requestBackgroundLocation: Boolean
    ) {
        val permissions = if (requestFineLocation) {
            mutableListOf(FINE_LOCATION_PERMISSION)
        } else {
            mutableListOf(COARSE_LOCATION_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestBackgroundLocation) {
            permissions.add(BACKGROUND_LOCATION_PERMISSION)
        }
        requestPermissions(activity, permissions.toTypedArray())
    }

    private fun requestPermissions(activity: Activity, permissions: Array<String>) {
        val permissionsToExplain = ArrayList<String>()
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                permissionsToExplain.add(permission)
            }
        }
        if (permissionsToExplain.isNotEmpty()) {
            listener?.onExplanationNeeded(permissionsToExplain)
        }
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            LOCATION_PERMISSIONS_REQUEST_CODE
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
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            listener?.let {
                val granted =
                    grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                it.onPermissionResult(granted)
            }
        }
    }

    companion object {

        private fun isPermissionGranted(context: Context, permission: String): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

        private fun isCoarseLocationPermissionGranted(context: Context): Boolean {
            return isPermissionGranted(context, COARSE_LOCATION_PERMISSION)
        }

        private fun isFineLocationPermissionGranted(context: Context): Boolean {
            return isPermissionGranted(context, FINE_LOCATION_PERMISSION)
        }

        private fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
            return isPermissionGranted(context, BACKGROUND_LOCATION_PERMISSION)
        }

        @JvmStatic
        fun areLocationPermissionsGranted(context: Context): Boolean {
            return isCoarseLocationPermissionGranted(context) ||
                isFineLocationPermissionGranted(context) ||
                (
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) &&
                        isBackgroundLocationPermissionGranted(context)
                    )
        }
    }
}

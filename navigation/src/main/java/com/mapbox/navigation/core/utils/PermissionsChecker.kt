package com.mapbox.navigation.core.utils

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.FOREGROUND_SERVICE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory

internal class PermissionsChecker(
    private val context: Context,
    private val sdkVersionProvider: () -> Int = { Build.VERSION.SDK_INT },
) {

    @SuppressLint("InlinedApi")
    fun hasForegroundServiceLocationPermissions(): Expected<String, Unit> {
        return if (sdkVersionProvider() < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ExpectedFactory.createValue(Unit)
        } else {
            val foregroundServicePermission = isPermissionGranted(FOREGROUND_SERVICE_LOCATION)
            val locationPermission = anyOfPermissionsGranted(
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
            )

            if (foregroundServicePermission && locationPermission) {
                ExpectedFactory.createValue(Unit)
            } else {
                val missingPermissions = mutableListOf<String>().apply {
                    if (!foregroundServicePermission) {
                        add(FOREGROUND_SERVICE_LOCATION)
                    }

                    if (!locationPermission) {
                        add("Any of $ACCESS_FINE_LOCATION or $ACCESS_COARSE_LOCATION")
                    }
                }.joinToString()
                ExpectedFactory.createError("Missing permissions: $missingPermissions")
            }
        }
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun anyOfPermissionsGranted(vararg permissions: String): Boolean {
        return permissions.any { isPermissionGranted(it) }
    }
}

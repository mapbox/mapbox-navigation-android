package com.mapbox.navigation.dropin.component.location

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Default experience for location permissions.
 *
 * @param componentActivityRef used for requesting location permissions
 * @param tripSessionStarterViewModel used to notify when location permissions are granted
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class LocationPermissionComponent(
    private val componentActivityRef: WeakReference<ComponentActivity>?,
    private val store: Store
) : UIComponent() {

    private val callback = ActivityResultCallback { permissions: Map<String, Boolean> ->
        val accessFineLocation = permissions[FINE_LOCATION_PERMISSIONS] ?: false
        val accessCoarseLocation = permissions[COARSE_LOCATION_PERMISSIONS] ?: false
        val granted = accessFineLocation || accessCoarseLocation
        onPermissionsResult(granted)
    }

    private val launcher = try {
        componentActivityRef?.get()?.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(), callback
        )
    } catch (illegalStateException: IllegalStateException) {
        logW(
            "Unable to request location permissions when view is created late in the " +
                "activity lifecycle. ${illegalStateException.message}",
            LOG_CATEGORY
        )
        null
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        componentActivityRef?.get()?.also { activity ->
            coroutineScope.launch {
                activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    checkPermissions(mapboxNavigation.navigationOptions.applicationContext)
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        launcher?.unregister()
    }

    private fun checkPermissions(applicationContext: Context) {
        val isGranted = PermissionsManager.areLocationPermissionsGranted(applicationContext)

        if (isGranted) {
            onPermissionsResult(isGranted)
        } else {
            launcher?.launch(LOCATION_PERMISSIONS)
            //  ActivityResultLauncher result is dispatched in ActivityResultCallback
        }
    }

    private fun onPermissionsResult(granted: Boolean) {
        store.dispatch(TripSessionStarterAction.OnLocationPermission(granted))
    }

    internal companion object {
        private val LOG_CATEGORY = this::class.java.simpleName

        private const val FINE_LOCATION_PERMISSIONS = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COARSE_LOCATION_PERMISSIONS = Manifest.permission.ACCESS_COARSE_LOCATION
        private val LOCATION_PERMISSIONS = arrayOf(
            FINE_LOCATION_PERMISSIONS,
            COARSE_LOCATION_PERMISSIONS
        )
    }
}

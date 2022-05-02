package com.mapbox.navigation.dropin.component.tripsession

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
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Store
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
        val accessFineLocation = permissions[FINE_LOCATION_PERMISSIONS]
            ?: false
        val accessCoarseLocation = permissions[COARSE_LOCATION_PERMISSIONS]
            ?: false
        val granted = accessFineLocation || accessCoarseLocation
        store.dispatch(TripSessionStarterAction.OnLocationPermission(granted))
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

        val isGranted = PermissionsManager.areLocationPermissionsGranted(
            mapboxNavigation.navigationOptions.applicationContext
        )

        if (isGranted) {
            // There can be a race condition between components and view models.
            // The view model attaches, and then launches a coroutine to collect actions.
            // The LocationPermissionComponent surfaces this issue because it is not owned by
            // a coordinator and flowable binder. This issue was also difficult to reproduce on
            // all devices. Launching a coroutine to update the state is a temporary solution.
            coroutineScope.launch {
                store.dispatch(TripSessionStarterAction.OnLocationPermission(true))
            }
        } else {
            launcher?.launch(LOCATION_PERMISSIONS)

            notifyGrantedOnForegrounded(mapboxNavigation.navigationOptions.applicationContext)
        }
    }

    /**
     * When the app is launched without location permissions. Run a check to see if location
     * permissions have been accepted yet. This will catch the case where a user will enable
     * location permissions through the app settings.
     */
    private fun notifyGrantedOnForegrounded(applicationContext: Context) {
        coroutineScope.launch {
            componentActivityRef?.get()?.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (!store.state.value.tripSession.isLocationPermissionGranted) {
                    val isGranted = PermissionsManager.areLocationPermissionsGranted(
                        applicationContext
                    )
                    if (isGranted) {
                        store.dispatch(TripSessionStarterAction.OnLocationPermission(true))
                    }
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        launcher?.unregister()
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

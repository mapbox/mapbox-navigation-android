package com.mapbox.navigation.dropin.permission

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

internal class LocationPermissionComponent(
    componentActivity: ComponentActivity,
    private val store: Store
) : UIComponent() {

    private val componentActivityRef = WeakReference(componentActivity)

    private val callback = ActivityResultCallback { _: Map<String, Boolean> ->
        store.dispatch(TripSessionStarterAction.RefreshLocationPermissions)
    }

    private val launcher = try {
        componentActivityRef.get()?.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            callback
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
                store.dispatch(TripSessionStarterAction.RefreshLocationPermissions)
            }
        } else {
            val fragActivity = componentActivityRef.get() as? FragmentActivity
            if (fragActivity != null) {
                PermissionsLauncherFragment.install(fragActivity, LOCATION_PERMISSIONS, callback)
            } else {
                launcher?.launch(LOCATION_PERMISSIONS)
            }
            notifyGrantedOnForegrounded()
        }
    }

    /**
     * When the app is launched without location permissions. Run a check to see if location
     * permissions have been accepted yet. This will catch the case where a user will enable
     * location permissions through the app settings or
     * when developers request location permissions themselves.
     */
    private fun notifyGrantedOnForegrounded() {
        coroutineScope.launch {
            componentActivityRef.get()?.lifecycle?.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                store.dispatch(TripSessionStarterAction.RefreshLocationPermissions)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        (componentActivityRef.get() as? FragmentActivity)?.also {
            PermissionsLauncherFragment.uninstall(it)
        }
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

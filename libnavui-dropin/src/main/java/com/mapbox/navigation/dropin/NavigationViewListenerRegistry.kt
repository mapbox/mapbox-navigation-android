package com.mapbox.navigation.dropin

import android.view.KeyEvent
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.map.MapClickBehavior
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelBehavior
import com.mapbox.navigation.dropin.component.maneuver.ManeuverBehavior
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewState
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewListenerRegistry(
    private val store: Store,
    private val maneuverSubscriber: ManeuverBehavior,
    private val infoPanelSubscriber: InfoPanelBehavior,
    private val mapClickSubscriber: MapClickBehavior,
    private val coroutineScope: CoroutineScope
) : View.OnKeyListener {
    private var listeners = mutableMapOf<NavigationViewListener, Job>()

    fun registerListener(listener: NavigationViewListener) {
        if (listeners.containsKey(listener)) return
        listeners[listener] = connectListener(listener)
    }

    fun unregisterListener(listener: NavigationViewListener) {
        listeners.remove(listener)?.cancel()
    }

    //region View.OnKeyListener

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            listeners.forEach { (listener, _) ->
                if (listener.onBackPressed()) return true
            }
        }
        return false
    }

    //endregion

    private fun connectListener(listener: NavigationViewListener): Job {
        return coroutineScope.launch {
            launch {
                store.select { it.destination?.point }.collect {
                    listener.onDestinationChanged(it)
                }
            }
            launch {
                store.select { it.previewRoutes }.collect {
                    when (it) {
                        is RoutePreviewState.Empty -> {
                            listener.onRouteFetchSuccessful(routes = emptyList())
                        }
                        is RoutePreviewState.Ready -> {
                            listener.onRouteFetchSuccessful(routes = it.routes)
                        }
                        is RoutePreviewState.Failed -> {
                            listener.onRouteFetchFailed(
                                reasons = it.reasons,
                                routeOptions = it.routeOptions
                            )
                        }
                        is RoutePreviewState.Canceled -> {
                            listener.onRouteFetchCanceled(
                                routeOptions = it.routeOptions,
                                routerOrigin = it.routerOrigin
                            )
                        }
                        is RoutePreviewState.Fetching -> {
                            if (0 < it.requestId) {
                                listener.onRouteFetching(requestId = it.requestId)
                            }
                        }
                    }
                }
            }
            launch {
                store.select { it.navigation }.collect {
                    listener.onNavigationStateChanged(it)
                }
            }
            launch {
                store.select { it.camera.cameraMode }.collect {
                    listener.onCameraModeChanged(it)
                }
            }
            launch {
                store.select { it.camera.cameraPadding }.collect {
                    listener.onCameraPaddingChanged(it)
                }
            }
            launch {
                store.select { it.audio.isMuted }.collect {
                    listener.onAudioGuidanceStateChanged(it)
                }
            }
            launch {
                infoPanelSubscriber
                    .infoPanelBehavior
                    .filterNotNull()
                    .collect { behavior ->
                        when (behavior) {
                            BottomSheetBehavior.STATE_HIDDEN -> listener.onInfoPanelHidden()
                            BottomSheetBehavior.STATE_EXPANDED -> listener.onInfoPanelExpanded()
                            BottomSheetBehavior.STATE_COLLAPSED -> listener.onInfoPanelCollapsed()
                            BottomSheetBehavior.STATE_DRAGGING -> listener.onInfoPanelDragging()
                            BottomSheetBehavior.STATE_SETTLING -> listener.onInfoPanelSettling()
                        }
                    }
            }

            launch {
                maneuverSubscriber
                    .maneuverBehavior
                    .collect { behavior ->
                        when (behavior) {
                            MapboxManeuverViewState.EXPANDED -> {
                                listener.onManeuverExpanded()
                            }
                            MapboxManeuverViewState.COLLAPSED -> {
                                listener.onManeuverCollapsed()
                            }
                        }
                    }
            }

            mapClickSubscriber.mapClickBehavior
                .onEach { listener.onMapClicked(it) }
                .launchIn(scope = this)
        }
    }

    private fun NavigationViewListener.onNavigationStateChanged(state: NavigationState) {
        when (state) {
            is NavigationState.FreeDrive -> onFreeDrive()
            is NavigationState.DestinationPreview -> onDestinationPreview()
            is NavigationState.RoutePreview -> onRoutePreview()
            is NavigationState.ActiveNavigation -> onActiveNavigation()
            is NavigationState.Arrival -> onArrival()
        }
    }

    private fun NavigationViewListener.onCameraModeChanged(mode: TargetCameraMode) {
        when (mode) {
            is TargetCameraMode.Idle -> onIdleCameraMode()
            is TargetCameraMode.Overview -> onOverviewCameraMode()
            is TargetCameraMode.Following -> onFollowingCameraMode()
        }
    }
}

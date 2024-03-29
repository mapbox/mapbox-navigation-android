package com.mapbox.navigation.dropin.navigationview

import androidx.annotation.VisibleForTesting
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

internal class NavigationViewListenerRegistry(
    private val store: Store,
    private val behavior: NavigationViewBehavior,
    private val coroutineScope: CoroutineScope,
) {
    private var listeners = mutableMapOf<NavigationViewListener, Job>()

    fun registerListener(listener: NavigationViewListener) {
        if (listeners.containsKey(listener)) return
        listeners[listener] = connectListener(listener)
    }

    fun unregisterListener(listener: NavigationViewListener) {
        listeners.remove(listener)?.cancel()
    }

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
                behavior
                    .infoPanelBehavior
                    .bottomSheetState
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
                behavior.infoPanelBehavior.slideOffset.collect(listener::onInfoPanelSlide)
            }
            launch {
                behavior
                    .maneuverBehavior
                    .maneuverViewState
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

            behavior.mapClickBehavior.onViewClicked
                .onEach { listener.onMapClicked(it) }
                .launchIn(scope = this)

            behavior.speedInfoBehavior.onViewClicked
                .onEach { listener.onSpeedInfoClicked(it) }
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

    @VisibleForTesting
    internal fun getRegisteredListeners() = listeners.keys
}

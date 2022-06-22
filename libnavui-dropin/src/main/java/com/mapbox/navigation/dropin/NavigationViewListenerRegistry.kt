package com.mapbox.navigation.dropin

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewListenerRegistry(
    private val store: Store,
    private val mapStyleLoader: MapStyleLoader,
    private val coroutineScope: CoroutineScope
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
                store.select { it.routes }.collect {
                    when (it) {
                        is RoutesState.Empty -> {
                            listener.onRouteFetchSuccessful(routes = emptyList())
                        }
                        is RoutesState.Ready -> {
                            listener.onRouteFetchSuccessful(routes = it.routes)
                        }
                        is RoutesState.Failed -> {
                            listener.onRouteFetchFailed(
                                reasons = it.reasons,
                                routeOptions = it.routeOptions
                            )
                        }
                        is RoutesState.Canceled -> {
                            listener.onRouteFetchCanceled(
                                routeOptions = it.routeOptions,
                                routerOrigin = it.routerOrigin
                            )
                        }
                        is RoutesState.Fetching -> {
                            listener.onRouteFetching(requestId = it.requestId)
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
                mapStyleLoader.loadedMapStyle.filterNotNull().collect {
                    listener.onMapStyleChanged(it)
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

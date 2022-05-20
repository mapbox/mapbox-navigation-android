@file:JvmName("NavigationCameraExFlowable")

package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)
fun NavigationCamera.flowNavigationCameraState(): Flow<NavigationCameraState> = callbackFlow {
    val observer = NavigationCameraStateChangedObserver { trySend(it) }
    registerNavigationCameraStateChangeObserver(observer)
    awaitClose { unregisterNavigationCameraStateChangeObserver(observer) }
}

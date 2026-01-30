package com.mapbox.navigation.base.reachability

import android.util.Log
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * This class is responsible for managing the lifecycle of the [PlatformReachability] instance.
 * It will start and stop the reachability instance based on the number of subscribers to the
 * [networkStatus] and [isReachable] properties.
 *
 * This class can be used as a singleton or as a property of a class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class SharedReachability {

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.NotReachable)

    /**
     * Returns state flow with network status.
     */
    val networkStatus = _networkStatus.asStateFlow()

    private val _isReachable = MutableStateFlow(false)

    /**
     * Returns state flow with network reachability state. True if network available.
     */
    val isReachable = _isReachable.asStateFlow()

    private val reachability: PlatformReachability = PlatformReachability.create(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val reachabilityChangedListener = PlatformReachabilityChanged { status ->
        onReachabilityUpdate(status)
    }

    init {
        onReachabilityUpdate(reachability.currentNetworkStatus())
        reachability.addListener(reachabilityChangedListener)
        scope.launch {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    reachability.removeListener(reachabilityChangedListener)
                }
            }
        }
    }

    private fun onReachabilityUpdate(status: NetworkStatus) {
        val isReachable = reachability.isReachable()
        _networkStatus.value = reachability.currentNetworkStatus()
        _isReachable.value = isReachable
        Log.i(TAG, "onReachabilityUpdate: ${status::class.simpleName}, $isReachable")
    }

    companion object {

        private const val TAG = "SharedReachability"
    }
}

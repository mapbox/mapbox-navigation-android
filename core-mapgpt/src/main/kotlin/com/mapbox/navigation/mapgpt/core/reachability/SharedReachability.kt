package com.mapbox.navigation.mapgpt.core.reachability

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch

/**
 * This class is responsible for managing the lifecycle of the [PlatformReachability] instance.
 * It will start and stop the reachability instance based on the number of subscribers to the
 * [networkStatus] and [isReachable] properties.
 *
 * This class can be used as a singleton or as a property of a class.
 */
class SharedReachability {

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.NotReachable)

    val networkStatus: StateFlow<NetworkStatus> = _networkStatus

    private val _isReachable = MutableStateFlow(false)
    val isReachable: StateFlow<Boolean> = _isReachable

    private val reachability: PlatformReachability = PlatformReachability.create(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val reachabilityChangedListener = PlatformReachabilityChanged { status ->
        onReachabilityUpdate(status)
    }

    init {
        scope.launch {
            onReachabilityUpdate(reachability.currentNetworkStatus())
        }
        combine(
            _networkStatus.subscriptionCount,
            _isReachable.subscriptionCount,
        ) { statusCount, reachableCount -> statusCount + reachableCount }
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .scan(false) { previous, current ->
                if (previous && !current) {
                    reachability.removeListener(reachabilityChangedListener)
                }
                if (current) {
                    reachability.addListener(reachabilityChangedListener)
                }
                current
            }
            .launchIn(scope)
    }

    private fun onReachabilityUpdate(status: NetworkStatus) {
        scope.launch {
            val isReachable = reachability.isReachable()
            _networkStatus.tryEmit(reachability.currentNetworkStatus())
            _isReachable.tryEmit(isReachable)
            Log.i(TAG, "onReachabilityUpdate: ${status::class.simpleName}, $isReachable")
        }
    }

    companion object {
        private const val TAG = "SharedReachability"
    }
}

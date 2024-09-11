package com.mapbox.navigation.utils.internal

import com.mapbox.common.NetworkStatus
import com.mapbox.common.ReachabilityChanged
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ConnectivityHandler(
    private val networkStatusChannel: Channel<Boolean>,
) : ReachabilityChanged {

    fun getNetworkStatusChannel(): ReceiveChannel<Boolean> = networkStatusChannel

    override fun run(status: NetworkStatus) {
        when (status) {
            NetworkStatus.NOT_REACHABLE -> {
                logD("NetworkStatus=$status", LOG_CATEGORY)
                networkStatusChannel.trySend(false).isSuccess
            }
            NetworkStatus.REACHABLE_VIA_WI_FI,
            NetworkStatus.REACHABLE_VIA_ETHERNET,
            NetworkStatus.REACHABLE_VIA_WWAN,
            -> {
                logD("NetworkStatus=$status", LOG_CATEGORY)
                networkStatusChannel.trySend(true).isSuccess
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "ConnectivityHandler"
    }
}

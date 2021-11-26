package com.mapbox.navigation.utils.internal

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.NetworkStatus
import com.mapbox.common.ReachabilityChanged
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ConnectivityHandler(
    private val logger: Logger,
    private val networkStatusChannel: Channel<Boolean>
) : ReachabilityChanged {

    fun getNetworkStatusChannel(): ReceiveChannel<Boolean> = networkStatusChannel

    override fun run(status: NetworkStatus) {
        when (status) {
            NetworkStatus.NOT_REACHABLE -> {
                logger.d(Tag(TAG), Message("NetworkStatus=$status"))
                networkStatusChannel.trySend(false).isSuccess
            }
            NetworkStatus.REACHABLE_VIA_WI_FI,
            NetworkStatus.REACHABLE_VIA_ETHERNET,
            NetworkStatus.REACHABLE_VIA_WWAN -> {
                logger.d(Tag(TAG), Message("NetworkStatus=$status"))
                networkStatusChannel.trySend(true).isSuccess
            }
        }
    }

    private companion object {
        private const val TAG = "MbxConnectivityHandler"
    }
}

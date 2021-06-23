package com.mapbox.navigation.utils.internal

import android.content.Context
import android.net.ConnectivityManager
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.MapboxSDKCommon.getContext
import com.mapbox.common.NetworkStatus
import com.mapbox.common.ReachabilityChanged
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ConnectivityHandler(
    private val logger: Logger,
    private val networkStatusChannel: Channel<Boolean>
) : ReachabilityChanged {
    private val cm =
        getContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
                // TODO: Remove workaround when fix from Common is available
                val activeNetwork = cm.activeNetworkInfo
                if (activeNetwork == null) {
                    logger.d(Tag(TAG), Message("NetworkStatus=${NetworkStatus.NOT_REACHABLE}"))
                    networkStatusChannel.trySend(false).isSuccess
                } else {
                    logger.d(Tag(TAG), Message("NetworkStatus=$status"))
                    networkStatusChannel.trySend(true).isSuccess
                }
            }
        }
    }

    private companion object {
        private const val TAG = "MbxConnectivityHandler"
    }
}

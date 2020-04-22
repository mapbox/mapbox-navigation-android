package com.mapbox.navigation.utils.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

data class NetworkStatus(val isNetworkAvailable: Boolean)

class NetworkStatusService(private val applicationContext: Context) {

    private val connectivityManager: ConnectivityManager = applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkStatusChannel = Channel<NetworkStatus>(Channel.CONFLATED)
    private var receiver: BroadcastReceiver? = null

    fun getNetworkStatusChannel(): ReceiveChannel<NetworkStatus> = networkStatusChannel

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)

            val callback: ConnectivityManager.NetworkCallback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network?) {
                        super.onAvailable(network)
                        networkStatusChannel.offer(NetworkStatus(true))
                    }

                    override fun onLost(network: Network?) {
                        super.onLost(network)
                        networkStatusChannel.offer(NetworkStatus(false))
                    }
                }
            connectivityManager.registerNetworkCallback(builder.build(), callback)
        } else {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val activeNetwork = connectivityManager.activeNetworkInfo
                    val isConnected = activeNetwork?.isConnectedOrConnecting ?: false
                    networkStatusChannel.offer(NetworkStatus(isConnected))
                }
            }
            applicationContext.registerReceiver(receiver, filter)
        }
    }

    fun cleanup() {
        if (receiver != null) {
            applicationContext.unregisterReceiver(receiver)
        }
    }
}

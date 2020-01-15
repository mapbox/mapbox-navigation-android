package com.mapbox.navigation.trip.service

import android.annotation.TargetApi
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

    fun getNetworkStatusChannel(): ReceiveChannel<NetworkStatus> = networkStatusChannel

    init {

        @TargetApi(11)
        when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            true -> {
                val builder = NetworkRequest.Builder()
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                builder.addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                val callback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
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
            }
            false -> {
                val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                val receiver: BroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val activeNetwork = connectivityManager.activeNetworkInfo
                        val isConnected = (activeNetwork != null &&
                                activeNetwork.isConnectedOrConnecting)
                        networkStatusChannel.offer(NetworkStatus(isConnected))
                    }
                }
                applicationContext.registerReceiver(receiver, filter)
            }
        }
    }
}

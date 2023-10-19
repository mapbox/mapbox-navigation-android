package com.mapbox.navigation.base.internal.route

interface ResponseDownloadedCallback {
    suspend fun onResponseDownloaded()
}

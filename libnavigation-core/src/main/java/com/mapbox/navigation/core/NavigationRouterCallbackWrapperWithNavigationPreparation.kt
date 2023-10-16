package com.mapbox.navigation.core

import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.ResponseDownloadedCallback

internal class NavigationRouterCallbackWrapperWithNavigationPreparation(
    private val wrapped: NavigationRouterCallback,
    private val prepareNavigationForParsing: suspend () -> Unit
    ):
    ResponseDownloadedCallback, NavigationRouterCallback by wrapped{
    override suspend fun onResponseDownloaded() {
        prepareNavigationForParsing()
    }
}
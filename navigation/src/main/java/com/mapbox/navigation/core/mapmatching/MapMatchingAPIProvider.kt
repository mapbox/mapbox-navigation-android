package com.mapbox.navigation.core.mapmatching

import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.MapboxServices
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import kotlinx.coroutines.Dispatchers

internal object MapMatchingAPIProvider {
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun provideMapMatchingAPI() = MapMatchingAPI(
        serialisationDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        httpServiceFactory = HttpServiceFactory::getInstance,
        sdkInformation = SdkInfoProvider.sdkInformation(),
        getCurrentAccessToken = { MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS) },
        skuTokenProvider = MapboxNavigationAccounts(),
    )
}

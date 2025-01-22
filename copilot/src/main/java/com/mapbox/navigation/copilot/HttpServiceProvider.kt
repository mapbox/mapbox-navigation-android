package com.mapbox.navigation.copilot

import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterface

internal object HttpServiceProvider {

    fun getInstance(): HttpServiceInterface = HttpServiceFactory.getInstance()
}

package com.mapbox.navigation.utils.internal

import com.mapbox.common.HttpServiceFactory

object HttpServiceFactoryWrapper {
    fun getInstance() = HttpServiceFactory.getInstance()
}

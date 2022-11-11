package com.mapbox.navigation.copilot

import com.mapbox.common.UploadServiceFactory
import com.mapbox.common.UploadServiceInterface

internal object UploadServiceInterfaceFactory {

    fun retrieveUploadServiceInterface(): UploadServiceInterface =
        UploadServiceFactory.getInstance()
}

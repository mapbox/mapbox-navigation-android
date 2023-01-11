package com.mapbox.navigation.ui.shield.internal

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.utils.internal.resource.load

internal object RoadShieldDownloader {

    private val resourceLoader get() = ResourceLoaderFactory.getInstance()

    suspend fun download(url: String): Expected<String, ByteArray> {
        val response = resourceLoader.load(url)

        return response.value?.let { responseData ->
            when (responseData.status) {
                ResourceLoadStatus.AVAILABLE -> {
                    val blob: ByteArray = responseData.data?.data ?: byteArrayOf()
                    if (blob.isNotEmpty()) {
                        ExpectedFactory.createValue(blob)
                    } else {
                        ExpectedFactory.createError("No data available.")
                    }
                }
                ResourceLoadStatus.UNAUTHORIZED ->
                    ExpectedFactory.createError("Your token cannot access this resource.")
                ResourceLoadStatus.NOT_FOUND ->
                    ExpectedFactory.createError("Resource is missing.")
                else ->
                    ExpectedFactory.createError("Unknown error (status: ${responseData.status}).")
            }
        } ?: ExpectedFactory.createError(response.error?.message ?: "No data available.")
    }

    private suspend fun ResourceLoader.load(url: String) = load(ResourceLoadRequest(url))
}

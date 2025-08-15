package com.mapbox.navigation.tripdata.shield

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.internal.utils.toByteArray
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoader
import com.mapbox.navigation.ui.utils.internal.resource.load

internal object RoadShieldDownloader {

    private val resourceLoader get() = ResourceLoaderFactory.getInstance()

    suspend fun download(url: String): Expected<String, ByteArray> {
        val response = resourceLoader.load(url)

        return response.value?.let { responseData ->
            when (responseData.status) {
                ResourceLoadStatus.AVAILABLE -> {
                    val blob = responseData.data?.data?.toByteArray() ?: byteArrayOf()
                    if (blob.isNotEmpty()) {
                        createValue(blob)
                    } else {
                        createError("No data available.")
                    }
                }
                ResourceLoadStatus.UNAUTHORIZED ->
                    createError("Your token cannot access this resource.")
                ResourceLoadStatus.NOT_FOUND ->
                    createError("Resource is missing.")
                else ->
                    createError("Unknown error (status: ${responseData.status}).")
            }
        } ?: createError(response.error?.message ?: "No data available.")
    }

    private suspend fun ResourceLoader.load(url: String) = load(ResourceLoadRequest(url))
}

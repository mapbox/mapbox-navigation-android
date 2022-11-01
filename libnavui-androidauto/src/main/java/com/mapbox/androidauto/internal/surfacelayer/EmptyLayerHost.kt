package com.mapbox.androidauto.internal.surfacelayer

import com.mapbox.maps.CustomLayerHost
import com.mapbox.maps.CustomLayerRenderParameters

// This layer fixes an issue on some Xiaomi devices that causes bitmap widgets not to render
class EmptyLayerHost : CustomLayerHost {

    override fun initialize() {
    }

    override fun render(parameters: CustomLayerRenderParameters) {
    }

    override fun contextLost() {
    }

    override fun deinitialize() {
    }
}

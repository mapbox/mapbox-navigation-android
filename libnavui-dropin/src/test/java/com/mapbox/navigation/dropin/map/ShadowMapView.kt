package com.mapbox.navigation.dropin.map

import android.content.Context
import android.util.AttributeSet
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.MapPlugin
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.compass.CompassPlugin
import io.mockk.mockk
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowView

@Implements(MapView::class)
class ShadowMapView : ShadowView() {

    private val compassMock = mockk<CompassPlugin>(relaxed = true)

    @RealObject
    private val mapView: MapView? = null

    @Implementation
    fun __constructor__(context: Context?) {
    }

    @Implementation
    fun __constructor__(context: Context?, mapInitOptions: MapInitOptions?) {
    }

    @Implementation
    fun __constructor__(context: Context?, attributeSet: AttributeSet?) {
    }

    @Implementation
    override fun __constructor__(
        context: Context,
        attributeSet: AttributeSet,
        defStyleAttr: Int
    ) {
    }

    @Implementation
    fun __constructor__(
        context: Context?,
        attributeSet: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
        mapInitOptions: MapInitOptions?
    ) {
    }

    @Implementation
    fun <T : MapPlugin> getPlugin(id: String): T? {
        if (id == Plugin.MAPBOX_COMPASS_PLUGIN_ID) {
            return compassMock as T
        }
        throw UnsupportedOperationException("No plugin for $id")
    }
}

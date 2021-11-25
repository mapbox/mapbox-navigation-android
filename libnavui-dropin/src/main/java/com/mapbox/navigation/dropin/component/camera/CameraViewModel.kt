package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

// This might be temporary just to test some camera stuff
internal class CameraViewModel : ViewModel() {

    // todo change this from a pair to a more explicit type
    private val _cameraUpdates: MutableSharedFlow<Pair<CameraOptions, MapAnimationOptions>> =
        MutableSharedFlow()

    val cameraUpdates: Flow<Pair<CameraOptions, MapAnimationOptions>> = _cameraUpdates

    fun consumeLocationUpdate(location: Location) = viewModelScope.launch {
        val animationOptions = MapAnimationOptions.Builder().duration(1500L).build()
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(location.longitude, location.latitude))
            .bearing(location.bearing.toDouble())
            .zoom(15.0)
            .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
            .build()
        _cameraUpdates.emit(Pair(cameraOptions, animationOptions))
    }
}

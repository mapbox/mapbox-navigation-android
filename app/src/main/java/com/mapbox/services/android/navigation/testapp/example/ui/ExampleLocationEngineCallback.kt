package com.mapbox.services.android.navigation.testapp.example.ui

import android.location.Location
import androidx.lifecycle.MutableLiveData
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.logger.MapboxLogger

class ExampleLocationEngineCallback(private val location: MutableLiveData<Location>) :
    LocationEngineCallback<LocationEngineResult> {

    override fun onSuccess(result: LocationEngineResult) {
        location.value = result.lastLocation
    }

    override fun onFailure(exception: Exception) {
        MapboxLogger.e(Message(exception.localizedMessage), exception)
    }
}

package com.mapbox.navigation.qa_test_app.car

import android.content.Intent
import android.content.res.Configuration
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.car.camera.RotateGlobeCamera
import com.mapbox.navigation.ui.car.map.MapboxCarMap
import com.mapbox.navigation.ui.car.map.MapboxCarOptions
import com.mapbox.navigation.utils.internal.logI

@OptIn(ExperimentalMapboxNavigationAPI::class)
class MainCarSession : Session() {

    private val currentStyle = MutableLiveData<String>()

    override fun onCreateScreen(intent: Intent): Screen {
        logI("onCreateScreen", LOG_CATEGORY)

        val mapInitOptions = MapInitOptions(carContext)
        val carOptions = MapboxCarOptions.Builder(mapInitOptions)
            .mapDayStyle(Style.TRAFFIC_DAY)
            .mapNightStyle(Style.TRAFFIC_NIGHT)
            .replayEnabled(true)
            .build()

        updateStyleData()

        val mapboxCarMap = MapboxCarMap(
            mapboxCarOptions = carOptions,
            carContext = carContext,
            lifecycle = lifecycle
        ).registerObserver(
            RotateGlobeCamera(this)
        )

        currentStyle.distinctUntilChanged().observe(this) {
            mapboxCarMap.updateMapStyle(it)
        }

        return MainCarScreen(carContext)
    }

    private fun updateStyleData() {
        currentStyle.value = if (carContext.isDarkMode) {
            Style.TRAFFIC_NIGHT
        } else {
            Style.TRAFFIC_DAY
        }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        logI("onCarConfigurationChanged ${carContext.isDarkMode}", LOG_CATEGORY)
        updateStyleData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        logI("onNewIntent $intent", LOG_CATEGORY)
    }

    private companion object {
        private const val LOG_CATEGORY = "MainCarSession"
    }
}

package com.mapbox.navigation.core.tests.activity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.common.module.provider.ModuleProviderArgument
import com.mapbox.navigation.base.TimeFormat.TWENTY_FOUR_HOURS
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.internal.trip.notification.TripNotificationInterceptorOwner
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.test.R
import com.mapbox.navigation.core.trip.service.MapboxTripService
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class TripServiceActivity : AppCompatActivity() {

    private val threadController = ThreadController()
    private var mainJobController = threadController.getMainScopeAndRootJob()
    private lateinit var tripNotification: TripNotification
    private lateinit var mapboxTripService: MapboxTripService
    private var textUpdateJob: Job = Job()
    private lateinit var startService: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_service)

        startService = findViewById(R.id.btnStart)

        tripNotification = MapboxModuleProvider.createModule(
            MapboxModuleType.NavigationTripNotification,
            ::paramsProvider,
        )

        mapboxTripService =
            MapboxTripService(applicationContext, tripNotification, threadController)

        startService.setOnClickListener {
            if (mapboxTripService.hasServiceStarted()) {
                stopService()
            } else {
                mapboxTripService.startService()
                changeText()
                startService.text = "Stop"
            }
        }
    }

    private fun paramsProvider(type: MapboxModuleType): Array<ModuleProviderArgument> {
        return when (type) {
            MapboxModuleType.NavigationTripNotification -> {
                val options = NavigationOptions.Builder(applicationContext)
                    .timeFormatType(TWENTY_FOUR_HOURS)
                    .build()

                arrayOf(
                    ModuleProviderArgument(NavigationOptions::class.java, options),
                    ModuleProviderArgument(
                        TripNotificationInterceptorOwner::class.java,
                        TripNotificationInterceptorOwner(),
                    ),
                    ModuleProviderArgument(
                        DistanceFormatter::class.java,
                        MapboxDistanceFormatter(options.distanceFormatterOptions),
                    ),
                )
            }
            else -> throw IllegalArgumentException("not supported: $type")
        }
    }

    private fun stopService() {
        textUpdateJob.cancel()
        mapboxTripService.stopService()
        startService.text = "Start"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        threadController.cancelAllNonUICoroutines()
        threadController.cancelAllUICoroutines()
    }

    private fun changeText() {
        textUpdateJob = mainJobController.scope.launch {
            while (isActive) {
                mapboxTripService.updateNotification(buildTripNotificationState(null))
                delay(1000L)
            }
        }
    }
}

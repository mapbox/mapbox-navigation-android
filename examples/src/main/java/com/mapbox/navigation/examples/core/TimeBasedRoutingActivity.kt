package com.mapbox.navigation.examples.core

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.show
import com.mapbox.navigation.examples.utils.extensions.toLatLng
import com.mapbox.navigation.examples.utils.extensions.toPoint
import com.mapbox.navigation.examples.utils.extensions.toast
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.activity_time_based_routing.btnClearRoutes
import kotlinx.android.synthetic.main.activity_time_based_routing.calendarButton
import kotlinx.android.synthetic.main.activity_time_based_routing.calendarView
import kotlinx.android.synthetic.main.activity_time_based_routing.dateTimeLabel
import kotlinx.android.synthetic.main.activity_time_based_routing.mapView
import kotlinx.android.synthetic.main.activity_time_based_routing.timeButton
import kotlinx.android.synthetic.main.activity_time_based_routing.timePicker
import kotlinx.android.synthetic.main.activity_time_based_routing.timeTypeRadioGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TimeBasedRoutingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null

    private var timeBaseRequest: TimeBaseRequest = TimeBaseRequest.DepartAt
    private lateinit var selectedDate: Date

    private val routesRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            navigationMapboxMap?.drawRoutes(routes)
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            toast("Request has failed")
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            toast("Requests has been canceled")
        }
    }

    private val coordinates = mutableListOf<Point>(DEFAULT_ORIGIN, DEFAULT_DESTINATION)

    companion object {
        private val DEFAULT_ORIGIN = Point.fromLngLat(-73.45716359811901, 40.66553736961296)
        private val DEFAULT_DESTINATION = Point.fromLngLat(-73.30353038099474, 40.82814921753268)
        private val DEFAULT_CAMERA_POSITION = CameraPosition.Builder()
            .target(TurfMeasurement.midpoint(DEFAULT_ORIGIN, DEFAULT_DESTINATION).toLatLng())
            .zoom(10.0)
            .build()
        private const val DEFAULT_CALENDAR_DAY = Calendar.FRIDAY
        private const val DEFAULT_HOUR = 13
        private const val DEFAULT_MIN = 0
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_based_routing)
        initViews()
        mapboxNavigation = MapboxNavigation(
            MapboxNavigation.defaultNavigationOptionsBuilder(
                this, Utils.getMapboxAccessToken(this)
            )
                .build()
        )
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            mapboxMap.cameraPosition = DEFAULT_CAMERA_POSITION
            navigationMapboxMap = NavigationMapboxMap(
                mapView,
                mapboxMap,
                this,
                true
            )

            setupCalendarView()
            setupDataPickerView()
            setupRadioGroup()
            updateDateTime()

            combineNewRouteRequest()
        }

        mapboxMap.addOnMapLongClickListener { latLnt ->
            if (coordinates.size > 25){
                toast("Limit of coordinates is 25")
                return@addOnMapLongClickListener false
            }
            coordinates.add(latLnt.toPoint())
            combineNewRouteRequest()
            return@addOnMapLongClickListener false
        }
    }

    private fun initViews() {
        calendarPickerVisibility(false)
        timePickerVisibility(false)
        mapView.getMapAsync(this)

        calendarButton.setOnClickListener {
            if (timePicker.show) {
                timePickerVisibility(false)
            }
            calendarPickerVisibility(!calendarView.show)
        }
        timeButton.setOnClickListener {
            if (calendarView.show) {
                calendarPickerVisibility(false)
            }
            timePickerVisibility(!timePicker.show)
        }
        btnClearRoutes.setOnClickListener {
            coordinates.clear()
            navigationMapboxMap?.hideRoute()
        }
    }

    private fun setupCalendarView() {
        calendarView.minDate = System.currentTimeMillis()

        val calendarInstance = Calendar.getInstance().also { calendar ->
            val now = Date(System.currentTimeMillis())
            calendar.time = now
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val moveDaysTo = when {
                dayOfWeek == DEFAULT_CALENDAR_DAY -> { // move to next friday
                    7
                }
                dayOfWeek > DEFAULT_CALENDAR_DAY -> { // move to friday that will be on next week
                    7 - dayOfWeek + DEFAULT_CALENDAR_DAY
                }
                else -> DEFAULT_CALENDAR_DAY - dayOfWeek // move to friday on this week
            }
            calendar.add(Calendar.DAY_OF_WEEK, moveDaysTo)
        }
        calendarView.date = calendarInstance.timeInMillis

        calendarView.setOnDateChangeListener { _, _, _, _ ->
            updateDateTime()
        }
    }

    private fun setupDataPickerView() {
        timePicker.currentHour = DEFAULT_HOUR
        timePicker.currentMinute = DEFAULT_MIN

        timePicker.setOnTimeChangedListener { _, _, _ ->
            updateDateTime()
        }
    }

    private fun setupRadioGroup() {
        timeTypeRadioGroup.check(R.id.radioDepartAt)

        timeTypeRadioGroup.setOnCheckedChangeListener { _, id ->
            radioGroupHasChanged(id)
        }
    }

    private fun calendarPickerVisibility(show: Boolean) {
        calendarView.show = show
    }

    private fun timePickerVisibility(show: Boolean) {
        timePicker.show = show
    }

    private fun radioGroupHasChanged(@IdRes id: Int) {
        timeBaseRequest = when (id) {
            R.id.radioArriveBy -> TimeBaseRequest.ArriveBy
            R.id.radioDepartAt -> TimeBaseRequest.DepartAt
            else -> throw IllegalStateException("Invalid radio button id: $id")
        }
        combineNewRouteRequest()
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance().also {
            it.time = Date(calendarView.date)
            it.set(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH),
                it.get(Calendar.DAY_OF_MONTH),
                timePicker.currentHour,
                timePicker.currentMinute
            )
        }
        selectedDate = calendar.time
        // dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
        dateTimeLabel.text = "${resources.getString(
            R.string.time_based_routing_selected_date_and_time_label,
            dateFormatter.format(selectedDate)
        )}(${dateFormatter.timeZone})"
        combineNewRouteRequest()
    }

    private fun combineNewRouteRequest() {
        if (coordinates.size < 2)
            return

        requestRoute(coordinates, timeBaseRequest)
    }

    private fun requestRoute(coordinates: List<Point>, timeBaseRequest: TimeBaseRequest) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(Utils.getMapboxAccessToken(applicationContext))
                .coordinates(coordinates)
                .alternatives(false)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .also {
                    val time = dateFormatter.format(selectedDate)
                    when (timeBaseRequest) {
                        TimeBaseRequest.DepartAt -> it.departAt(time)
                        TimeBaseRequest.ArriveBy -> it.arriveBy(time)
                    }
                }
                .build(),
            routesRequestCallback
        )
    }

    private enum class TimeBaseRequest {
        DepartAt,
        ArriveBy;
    }
}
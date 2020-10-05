package com.mapbox.navigation.carbon.examples

import android.Manifest.permission
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMap.OnMapLoadErrorListener
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl
import com.mapbox.maps.plugin.animation.animator.*
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.gesture.GesturePluginImpl
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationComponentPlugin
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.carbon.examples.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.carbon.examples.LocationPermissionHelper.Companion.areLocationPermissionsGranted
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import kotlinx.android.synthetic.main.layout_camera_animations.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class CameraAnimationsActivity: AppCompatActivity(), PermissionsListener, OnAnimationButtonClicked {

    private var  locationComponent: LocationComponentPlugin? = null
    private lateinit var  mapboxMap: MapboxMap
    private lateinit var  mapCamera: CameraAnimationsPluginImpl
    private lateinit var mapboxNavigation: MapboxNavigation
    private val permissionsHelper = LocationPermissionHelper(this)
    private val locationEngineCallback: MyLocationEngineCallback = MyLocationEngineCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_camera_animations)
        mapboxMap = mapView.getMapboxMap()
        locationComponent = getLocationComponent()
        mapCamera = getMapCamera()

        if (areLocationPermissionsGranted(this)) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionsHelper.requestLocationPermissions(this)
        }
    }

    private fun init() {
        initAnimations()
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        val navigationOptions = defaultNavigationOptionsBuilder(this, getMapboxAccessTokenFromResources())
            .locationEngine(LocationEngineProvider.getBestLocationEngine(this))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions)
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS, object: Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                initializeLocationComponent(style)
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)
            }
        }, object: OnMapLoadErrorListener {
            override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                Timber.e("Error loading map: %s", mapViewLoadError.name)
            }
        })
    }

    private fun initAnimations() {
        val adapter = AnimationAdapter(this, this)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        animationsList.layoutManager = manager
        animationsList.adapter = adapter
    }

    override fun onButtonClicked(animationType: AnimationType) {
        val location = locationComponent?.lastKnownLocation
        if (location != null) {
            when (animationType) {
                AnimationType.Animation1 -> {
                    val target = CameraCenterAnimator.create(
                        Point.fromLngLat(location.longitude, location.latitude)
                    ) {
                        duration = 2000
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val zoom = CameraZoomAnimator.create(16.35) {
                        startDelay = 600
                        duration = 3000
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val bearing = CameraBearingAnimator.create(location.bearing.toDouble()) {
                        startDelay = 1400
                        duration = 1800
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val pitch = CameraPitchAnimator.create(40.0) {
                        startDelay = 2300
                        duration = 1200
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    mapCamera.registerAnimators(target, zoom, bearing, pitch)
                    val set = AnimatorSet()
                    set.playTogether(target, zoom, bearing, pitch)
                    set.start()

                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation1",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation2 -> {
                    val target = CameraCenterAnimator.create(
                        Point.fromLngLat(location.longitude, location.latitude)
                    ) {
                        duration = 1800
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val zoom = CameraZoomAnimator.create(12.35) {
                        startDelay = 0
                        duration = 1800
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val bearing = CameraBearingAnimator.create(0.0) {
                        startDelay = 600
                        duration = 1800
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    val pitch = CameraPitchAnimator.create(0.0) {
                        startDelay = 0
                        duration = 1200
                        interpolator = PathInterpolatorCompat.create(0.4f, 0f, 0.4f, 1f)
                    }
                    mapCamera.registerAnimators(target, zoom, bearing, pitch)
                    val set = AnimatorSet()
                    set.playTogether(target, zoom, bearing, pitch)
                    set.start()
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation2",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation3 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation3",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation4 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation4",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation5 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation5",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation6 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation6",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation7 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation7",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation8 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation8",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation9 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation9",
                            Toast.LENGTH_SHORT).show()
                }
                AnimationType.Animation10 -> {
                    Toast
                        .makeText(this@CameraAnimationsActivity,
                            "Animation10",
                            Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    private fun initializeLocationComponent(style: Style) {
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false) //SBNOTE: I think this should be false eventually
            .build()
        locationComponent?.let {
            it.activateLocationComponent(activationOptions)
            it.enabled = true
            it.renderMode = RenderMode.COMPASS
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getLocationComponent(): LocationComponentPlugin? {
        return mapView.getPlugin(LocationComponentPlugin::class.java)
    }

    private fun getMapCamera(): CameraAnimationsPluginImpl {
        return mapView.getCameraAnimationsPlugin()
    }

    private fun getGesturePlugin(): GesturePluginImpl? {
        return mapView.getPlugin(GesturePluginImpl::class.java)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "This app needs location and storage permissions in order to show its functionality.", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when {
            requestCode == LOCATION_PERMISSIONS_REQUEST_CODE -> {
                permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
            grantResults.isNotEmpty() -> {
                init()
            }
            else -> {
                Toast.makeText(this, "You didn't grant storage and/or location permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            requestPermissionIfNotGranted(permission.WRITE_EXTERNAL_STORAGE)
        } else {
            Toast.makeText(this, "Uou didn't grant location permissions.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionIfNotGranted(permission: String) {
        val permissionsNeeded: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(permission)
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 10)
        } else {
            init()
        }
    }

    companion object {
        class MyLocationEngineCallback(activity: CameraAnimationsActivity): LocationEngineCallback<LocationEngineResult> {
            private val activityRef = WeakReference<CameraAnimationsActivity>(activity)

            override fun onSuccess(result: LocationEngineResult?) {
                val activity = activityRef.get()
                activity?.locationComponent?.let { locComponent ->
                    val location = result?.lastLocation
                    location?.let { loc ->
                        val point = Point.fromLngLat(loc.longitude, loc.latitude)
                        val cameraOptions = CameraOptions.Builder().center(point).zoom(13.0).build()
                        activity.mapboxMap.jumpTo(cameraOptions)
                        locComponent.forceLocationUpdate(location)
                    } ?: Timber.e("Location from the result is null")
                } ?: Timber.e("Location Component cannot be null")
            }

            override fun onFailure(exception: Exception) {
                Timber.i(exception)
            }
        }
    }
}

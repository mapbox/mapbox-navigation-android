package com.mapbox.navigation.dropin.component.map

import android.view.MotionEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.removeOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MapEventProducer(private val mapView: MapView) : DefaultLifecycleObserver {

    private val jobControl = InternalJobControlFactory.createMainScopeJobControl()

    private val _mapStyleSink: MutableSharedFlow<Style> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1
    )
    val mapStyleUpdates: Flow<Style> = _mapStyleSink

    private val _mapLongClickSink: MutableSharedFlow<Point> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mapLongClicks: Flow<Point> = _mapLongClickSink

    private val _mapClickSink: MutableSharedFlow<Point> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mapClicks: Flow<Point> = _mapClickSink

    private val _positionChangeSink: MutableSharedFlow<Point> = MutableSharedFlow()
    val positionChanges: Flow<Point> = _positionChangeSink

    private val _mapMovementsSink: MutableSharedFlow<MotionEvent> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val mapMovements: Flow<MotionEvent> = _mapMovementsSink

    private val onStyleLoadedListener = OnStyleLoadedListener {
        ifNonNull(mapView.getMapboxMap().getStyle()) {
            _mapStyleSink.tryEmit(it)
        }
    }

    private val onMapLongClickListener = OnMapLongClickListener { point ->
        _mapLongClickSink.tryEmit(point)
        false
    }

    private val onMapClickListener = OnMapClickListener { point ->
        _mapClickSink.tryEmit(point)
        false
    }

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        jobControl.scope.launch {
            _positionChangeSink.emit(point)
        }
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveBegin(detector: MoveGestureDetector) { }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            _mapMovementsSink.tryEmit(detector.currentEvent)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapView.getMapboxMap().addOnStyleLoadedListener(onStyleLoadedListener)
        mapView.getMapboxMap().addOnMapLongClickListener(onMapLongClickListener)
        mapView.getMapboxMap().addOnMapClickListener(onMapClickListener)
        mapView.location.addOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapView.getMapboxMap().addOnMoveListener(onMoveListener)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapView.getMapboxMap().removeOnStyleLoadedListener(onStyleLoadedListener)
        mapView.getMapboxMap().removeOnMapLongClickListener(onMapLongClickListener)
        mapView.getMapboxMap().removeOnMapClickListener(onMapClickListener)
        mapView.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapView.getMapboxMap().removeOnMoveListener(onMoveListener)
    }
}

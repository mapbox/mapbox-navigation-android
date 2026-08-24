package com.mapbox.navigation.ui.maps.camera.utils

import android.animation.Animator
import android.animation.AnimatorSet
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorsFactory
import com.mapbox.maps.plugin.animation.animator.CameraAnimator
import com.mapbox.maps.plugin.animation.getCameraAnimatorsFactory
import kotlin.math.hypot
import kotlin.math.ln

internal fun normalizeProjection(projectedDistance: Double): Double {
    return ((ln((projectedDistance / 1000.0) + 0.24) + 2.1) * 1000.0)
}

internal fun createAnimatorSet(animators: List<Animator>) = AnimatorSet().apply {
    playTogether(*(animators.toTypedArray()))
}

internal fun createAnimatorSetWith(animators: Array<CameraAnimator<*>>) = AnimatorSet().apply {
    playTogether(*(animators))
}

internal fun projectedDistance(
    mapboxMap: MapboxMap,
    currentPoint: Point,
    targetPoint: Point,
    targetZL: Double,
): Double {
    return hypot(
        mapboxMap.project(currentPoint, targetZL).x - mapboxMap.project(targetPoint, targetZL).x,
        mapboxMap.project(targetPoint, targetZL).y - mapboxMap.project(targetPoint, targetZL).y,
    )
}

internal fun screenDistanceFromMapCenterToTarget(
    mapboxMap: MapboxMap,
    currentCenter: Point,
    targetCenter: Point,
): Double {
    val currentCenterScreenCoordinate = mapboxMap.pixelForCoordinate(currentCenter)
    val locationScreenCoordinate = mapboxMap.pixelForCoordinate(targetCenter)
    return hypot(
        currentCenterScreenCoordinate.x - locationScreenCoordinate.x,
        currentCenterScreenCoordinate.y - locationScreenCoordinate.y,
    )
}

internal fun CameraAnimationsPlugin.getAnimatorsFactory(): CameraAnimatorsFactory =
    getCameraAnimatorsFactory()

package com.mapbox.navigation.ui.androidauto.navigation.lanes

import android.content.Context
import android.graphics.Color
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.Step
import com.mapbox.navigation.tripdata.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.tripdata.maneuver.model.Lane

/**
 * This class generates a [CarLanesImage] needed for the lane guidance in android auto.
 */
class CarLanesImageRenderer(
    context: Context,
    @ColorInt
    val background: Int = Color.TRANSPARENT,
    val options: CarLaneIconOptions = CarLaneIconOptions.Builder().build(context),
) {
    private val carLaneIconRenderer = CarLaneIconRenderer(context)
    private val laneIconsApi = MapboxLaneIconsApi()
    private val carLaneIconMapper = CarLaneMapper()
    private val cache = LruCache<Lane, CarLanesImage>(1)

    /**
     * Create the images needed to show lane guidance.
     *
     * @param lane retrieve the lane guidance through the [MapboxManeuverApi]
     * @return the lanes image, null when there is no lange guidance
     */
    fun renderLanesImage(lane: Lane?): CarLanesImage? {
        return lane?.let {
            cache.get(lane)?.also {
                return it
            }

            val img = CarLanesImage(
                lanes = carLaneIconMapper.mapLanes(lane),
                carIcon = lanesCarIcon(lane),
            )
            cache.put(lane, img)
            img
        }
    }

    private fun lanesCarIcon(laneGuidance: Lane): CarIcon {
        val carLaneIcons = laneGuidance.allLanes.map { laneIndicator ->
            val laneIcon = laneIconsApi.getTurnLane(laneIndicator)
            CarLaneIcon(
                laneIcon,
                laneIndicator.isActive,
            )
        }
        return carLaneIconRenderer.renderLanesIcons(
            carLaneIcons,
            background,
            options,
        )
    }
}

/**
 * When building a [Step] you can generate the lane guidance images here.
 */
fun Step.Builder.useMapboxLaneGuidance(
    imageGenerator: CarLanesImageRenderer,
    laneGuidance: Lane?,
) = apply {
    val lanesImage = imageGenerator.renderLanesImage(laneGuidance)
    if (lanesImage != null) {
        lanesImage.lanes.forEach { addLane(it) }
        setLanesImage(lanesImage.carIcon)
    }
}

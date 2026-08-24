package com.mapbox.navigation.ui.androidauto.action

import androidx.annotation.DrawableRes
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.navigation.CarCameraMode
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import kotlinx.coroutines.launch

class MapboxMapActionStrip(
    private val screen: Screen,
    private val carNavigationCamera: CarNavigationCamera,
) {
    init {
        screen.lifecycleScope.launch {
            screen.repeatOnLifecycle(Lifecycle.State.STARTED) {
                carNavigationCamera.nextCameraMode.collect { screen.invalidate() }
            }
        }
    }

    fun build(): ActionStrip {
        val mapActionStripBuilder = ActionStrip.Builder()
            .addAction(buildPanAction())
        val nextCameraMode = carNavigationCamera.nextCameraMode.value
        when {
            nextCameraMode == CarCameraMode.FOLLOWING -> mapActionStripBuilder.addAction(
                buildRecenterAction(),
            )
            nextCameraMode == CarCameraMode.OVERVIEW -> mapActionStripBuilder.addAction(
                buildOverviewAction(),
            )
            !carNavigationCamera.followingZoomUpdatesAllowed() -> mapActionStripBuilder.addAction(
                buildRecenterAction(),
            )
        }

        return mapActionStripBuilder
            .addAction(buildZoomInAction(carNavigationCamera))
            .addAction(buildZoomOutAction(carNavigationCamera))
            .build()
    }

    private fun buildPanAction() = Action.Builder(Action.PAN)
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_recenter_24,
                ),
            ).build(),
        )
        .build()

    private fun buildZoomInAction(carNavigationCamera: CarNavigationCamera) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_zoom_in_24,
                ),
            ).build(),
        )
        .setOnClickListener {
            carNavigationCamera.zoomUpdatesAllowed(false)
            carNavigationCamera.zoomInAction()
        }
        .build()

    private fun buildZoomOutAction(carNavigationCamera: CarNavigationCamera) = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    screen.carContext,
                    R.drawable.ic_zoom_out_24,
                ),
            ).build(),
        )
        .setOnClickListener {
            carNavigationCamera.zoomUpdatesAllowed(false)
            carNavigationCamera.zoomOutAction()
        }
        .build()

    private fun buildRecenterAction() =
        buildCameraAction(R.drawable.ic_recenter_24, CarCameraMode.FOLLOWING)

    private fun buildOverviewAction() =
        buildCameraAction(R.drawable.ic_route_overview, CarCameraMode.OVERVIEW)

    private fun buildCameraAction(@DrawableRes iconId: Int, carCameraMode: CarCameraMode): Action {
        return Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        screen.carContext,
                        iconId,
                    ),
                ).build(),
            )
            .setOnClickListener {
                carNavigationCamera.zoomUpdatesAllowed(true)
                carNavigationCamera.updateCameraMode(carCameraMode)
            }
            .build()
    }
}

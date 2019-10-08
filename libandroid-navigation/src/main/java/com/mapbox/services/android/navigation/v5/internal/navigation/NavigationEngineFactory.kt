package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.services.android.navigation.v5.navigation.camera.Camera
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera
import com.mapbox.services.android.navigation.v5.offroute.OffRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector
import com.mapbox.services.android.navigation.v5.route.FasterRoute
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector
import com.mapbox.services.android.navigation.v5.snap.Snap
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute

class NavigationEngineFactory {

    private var offRouteEngine: OffRoute = OffRouteDetector()
    private var fasterRouteEngine: FasterRoute = FasterRouteDetector()
    private var snapEngine: Snap = SnapToRoute()
    private var cameraEngine: Camera = SimpleCamera()

    fun retrieveOffRouteEngine() =
            offRouteEngine

    fun updateOffRouteEngine(offRouteEngine: OffRoute) {
        this.offRouteEngine = offRouteEngine
    }

    fun retrieveFasterRouteEngine() =
            fasterRouteEngine

    fun updateFasterRouteEngine(fasterRouteEngine: FasterRoute) {
        this.fasterRouteEngine = fasterRouteEngine
    }

    fun retrieveSnapEngine() = snapEngine

    fun updateSnapEngine(snapEngine: Snap) {
        this.snapEngine = snapEngine
    }

    fun retrieveCameraEngine() = cameraEngine

    fun updateCameraEngine(cameraEngine: Camera) {
        this.cameraEngine = cameraEngine
    }
}

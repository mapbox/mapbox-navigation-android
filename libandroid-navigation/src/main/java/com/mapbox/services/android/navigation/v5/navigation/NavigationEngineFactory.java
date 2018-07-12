package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;

class NavigationEngineFactory {

  private OffRoute offRouteEngine;
  private FasterRoute fasterRouteEngine;
  private Snap snapEngine;
  private Camera cameraEngine;

  NavigationEngineFactory() {
    initializeDefaultEngines();
  }

  OffRoute retrieveOffRouteEngine() {
    return offRouteEngine;
  }

  void updateOffRouteEngine(OffRoute offRouteEngine) {
    this.offRouteEngine = offRouteEngine;
  }

  FasterRoute retrieveFasterRouteEngine() {
    return fasterRouteEngine;
  }

  void updateFasterRouteEngine(FasterRoute fasterRouteEngine) {
    this.fasterRouteEngine = fasterRouteEngine;
  }

  Snap retrieveSnapEngine() {
    return snapEngine;
  }

  void updateSnapEngine(Snap snapEngine) {
    this.snapEngine = snapEngine;
  }

  Camera retrieveCameraEngine() {
    return cameraEngine;
  }

  void updateCameraEngine(Camera cameraEngine) {
    this.cameraEngine = cameraEngine;
  }

  void clearEngines() {
    offRouteEngine = null;
    fasterRouteEngine = null;
    snapEngine = null;
    cameraEngine = null;
  }

  private void initializeDefaultEngines() {
    cameraEngine = new SimpleCamera();
    snapEngine = new SnapToRoute();
    offRouteEngine = new OffRouteDetector();
    fasterRouteEngine = new FasterRouteDetector();
  }
}

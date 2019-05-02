package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.internal.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.internal.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.internal.snap.SnapToRoute;

public class NavigationEngineFactory {

  private OffRoute offRouteEngine;
  private FasterRoute fasterRouteEngine;
  private Snap snapEngine;
  private Camera cameraEngine;

  public NavigationEngineFactory() {
    initializeDefaultEngines();
  }

  public OffRoute retrieveOffRouteEngine() {
    return offRouteEngine;
  }

  public void updateOffRouteEngine(OffRoute offRouteEngine) {
    if (offRouteEngine == null) {
      return;
    }
    this.offRouteEngine = offRouteEngine;
  }

  public FasterRoute retrieveFasterRouteEngine() {
    return fasterRouteEngine;
  }

  public void updateFasterRouteEngine(FasterRoute fasterRouteEngine) {
    if (fasterRouteEngine == null) {
      return;
    }
    this.fasterRouteEngine = fasterRouteEngine;
  }

  public Snap retrieveSnapEngine() {
    return snapEngine;
  }

  public void updateSnapEngine(Snap snapEngine) {
    if (snapEngine == null) {
      return;
    }
    this.snapEngine = snapEngine;
  }

  public Camera retrieveCameraEngine() {
    return cameraEngine;
  }

  public void updateCameraEngine(Camera cameraEngine) {
    if (cameraEngine == null) {
      return;
    }
    this.cameraEngine = cameraEngine;
  }

  private void initializeDefaultEngines() {
    cameraEngine = new SimpleCamera();
    snapEngine = new SnapToRoute();
    offRouteEngine = new OffRouteDetector();
    fasterRouteEngine = new FasterRouteDetector();
  }
}

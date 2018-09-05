package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;

class NavigationEngineFactory {

  private OffRoute offRouteEngine;
  private FasterRoute fasterRouteEngine;
  private Snap snapEngine;
  private Camera cameraEngine;

  NavigationEngineFactory(Navigator navigator) {
    initializeDefaultEngines(navigator);
  }

  // For testing purposes only
  NavigationEngineFactory() {
    cameraEngine = new SimpleCamera();
    fasterRouteEngine = new FasterRouteDetector();
    snapEngine = new Snap() {
      @Override
      public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
        return location;
      }
    };
    offRouteEngine = new OffRoute() {
      @Override
      public boolean isUserOffRoute(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
        return false;
      }
    };
  }

  OffRoute retrieveOffRouteEngine() {
    return offRouteEngine;
  }

  void updateOffRouteEngine(OffRoute offRouteEngine) {
    if (offRouteEngine == null) {
      return;
    }
    this.offRouteEngine = offRouteEngine;
  }

  FasterRoute retrieveFasterRouteEngine() {
    return fasterRouteEngine;
  }

  void updateFasterRouteEngine(FasterRoute fasterRouteEngine) {
    if (fasterRouteEngine == null) {
      return;
    }
    this.fasterRouteEngine = fasterRouteEngine;
  }

  Snap retrieveSnapEngine() {
    return snapEngine;
  }

  void updateSnapEngine(Snap snapEngine) {
    if (snapEngine == null) {
      return;
    }
    this.snapEngine = snapEngine;
  }

  Camera retrieveCameraEngine() {
    return cameraEngine;
  }

  void updateCameraEngine(Camera cameraEngine) {
    if (cameraEngine == null) {
      return;
    }
    this.cameraEngine = cameraEngine;
  }

  private void initializeDefaultEngines(Navigator navigator) {
    cameraEngine = new SimpleCamera();
    snapEngine = new SnapToRoute(navigator);
    offRouteEngine = new OffRouteDetector(navigator);
    fasterRouteEngine = new FasterRouteDetector();
  }
}

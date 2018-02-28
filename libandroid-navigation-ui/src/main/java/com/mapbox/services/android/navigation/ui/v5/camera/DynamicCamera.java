package com.mapbox.services.android.navigation.ui.v5.camera;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

public class DynamicCamera extends SimpleCamera {

  private static final int MAX_CAMERA_TILT = 50;

  private MapboxMap mapboxMap;
  private CameraPosition cameraPosition;

  public DynamicCamera(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  @Override
  public Point target(RouteInformation routeInformation) {
    if (validLocationAndProgress(routeInformation)) {
      createCameraPosition(routeInformation.location(), routeInformation.routeProgress());
      LatLng target = cameraPosition.target;
      return Point.fromLngLat(target.getLongitude(), target.getLatitude());
    }
    return super.target(routeInformation);
  }

  @Override
  public double tilt(RouteInformation routeInformation) {
    RouteProgress progress = routeInformation.routeProgress();
    if (progress != null) {
      double distanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
      return createTilt(distanceRemaining);
    }
    return super.tilt(routeInformation);
  }

  @Override
  public double zoom(RouteInformation routeInformation) {
    if (validLocationAndProgress(routeInformation)) {
      createCameraPosition(routeInformation.location(), routeInformation.routeProgress());
      return checkZoomLimit(cameraPosition);
    }
    return super.zoom(routeInformation);
  }

  private double createTilt(double distanceRemaining) {
    double tilt = distanceRemaining / 5;
    if (tilt > MAX_CAMERA_TILT) {
      return MAX_CAMERA_TILT;
    }
    return Math.round(tilt);
  }

  private boolean validLocationAndProgress(RouteInformation routeInformation) {
    return routeInformation.location() != null && routeInformation.routeProgress() != null;
  }

  private void createCameraPosition(Location location, RouteProgress routeProgress) {
    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();

    if (upComingStep != null) {
      Point stepManeuverPoint = upComingStep.maneuver().location();

      List<LatLng> latLngs = new ArrayList<>();
      latLngs.add(new LatLng(stepManeuverPoint.latitude(), stepManeuverPoint.longitude()));
      latLngs.add(new LatLng(location));

      if (latLngs.size() < 1) {
        return;
      }

      LatLngBounds cameraBounds = new LatLngBounds.Builder()
        .includes(latLngs)
        .build();

      int[] padding = {0, 0, 0, 0};
      cameraPosition = mapboxMap.getCameraForLatLngBounds(cameraBounds, padding);
    }
  }

  private double checkZoomLimit(CameraPosition position) {
    if (position.zoom > 16) {
      return 16;
    } else if (position.zoom < 12) {
      return 12;
    } else {
      return position.zoom;
    }
  }
}

package com.mapbox.services.android.navigation.ui.v5.camera;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

public class DynamicCamera extends SimpleCamera {

  private static final double MAX_CAMERA_TILT = 50d;
  private static final double MIN_CAMERA_TILT = 35d;
  private static final double MAX_CAMERA_ZOOM = 16d;
  private static final double MIN_CAMERA_ZOOM = 12d;

  private MapboxMap mapboxMap;
  private LegStep currentStep;
  private boolean hasPassedLowAlertLevel;
  private boolean hasPassedMediumAlertLevel;
  private boolean hasPassedHighAlertLevel;
  private boolean forceUpdateZoom;

  public DynamicCamera(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  @Override
  public Point target(RouteInformation routeInformation) {
    if (routeInformation.location() != null) {
      Location target = routeInformation.location();
      return Point.fromLngLat(target.getLongitude(), target.getLatitude());
    } else if (routeInformation.route() != null) {
      return super.target(routeInformation);
    }
    // Without route or location info, return the current position
    LatLng currentTarget = mapboxMap.getCameraPosition().target;
    return Point.fromLngLat(currentTarget.getLongitude(), currentTarget.getLongitude());
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
    if (validLocationAndProgress(routeInformation) && shouldUpdateZoom(routeInformation)) {
      return createZoom(routeInformation);
    } else if (routeInformation.route() != null) {
      return super.zoom(routeInformation);
    }
    return mapboxMap.getCameraPosition().zoom;
  }


  /**
   * Called when the zoom level should force update on the next usage
   * of {@link DynamicCamera#zoom(RouteInformation)}.
   */
  public void forceResetZoomLevel() {
    forceUpdateZoom = true;
  }

  /**
   * Creates a tilt value based on the distance remaining for the current {@link LegStep}.
   * <p>
   * Checks if the calculated value is within the set min / max bounds.
   *
   * @param distanceRemaining from the current step
   * @return tilt within set min / max bounds
   */
  private double createTilt(double distanceRemaining) {
    double tilt = distanceRemaining / 5;
    if (tilt > MAX_CAMERA_TILT) {
      return MAX_CAMERA_TILT;
    } else if (tilt < MIN_CAMERA_TILT) {
      return MIN_CAMERA_TILT;
    }
    return Math.round(tilt);
  }

  /**
   * Creates a zoom value based on the result of {@link MapboxMap#getCameraForLatLngBounds(LatLngBounds, int[])}.
   * <p>
   * 0 zoom is the world view, while 22 (default max threshold) is the closest you can position
   * the camera to the map.
   *
   * @param routeInformation for current location and progress
   * @return zoom within set min / max bounds
   */
  private double createZoom(RouteInformation routeInformation) {
    CameraPosition position = createCameraPosition(routeInformation.location(), routeInformation.routeProgress());
    if (position.zoom > MAX_CAMERA_ZOOM) {
      return MAX_CAMERA_ZOOM;
    } else if (position.zoom < MIN_CAMERA_ZOOM) {
      return MIN_CAMERA_ZOOM;
    }
    return position.zoom;
  }

  /**
   * Creates a camera position with the current location and upcoming maneuver location.
   * <p>
   * Using {@link MapboxMap#getCameraForLatLngBounds(LatLngBounds, int[])} with a {@link LatLngBounds}
   * that includes the current location and upcoming maneuver location.
   *
   * @param location      for current location
   * @param routeProgress for upcoming maneuver location
   * @return camera position that encompasses both locations
   */
  private CameraPosition createCameraPosition(Location location, RouteProgress routeProgress) {
    LegStep upComingStep = routeProgress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      Point stepManeuverPoint = upComingStep.maneuver().location();

      List<LatLng> latLngs = new ArrayList<>();
      LatLng currentLatLng = new LatLng(location);
      LatLng maneuverLatLng = new LatLng(stepManeuverPoint.latitude(), stepManeuverPoint.longitude());
      latLngs.add(currentLatLng);
      latLngs.add(maneuverLatLng);

      if (latLngs.size() < 1 || currentLatLng.equals(maneuverLatLng)) {
        return mapboxMap.getCameraPosition();
      }

      LatLngBounds cameraBounds = new LatLngBounds.Builder()
        .includes(latLngs)
        .build();

      int[] padding = {0, 0, 0, 0};
      CameraPosition positionForLatLngBounds = mapboxMap.getCameraForLatLngBounds(cameraBounds, padding);
      if (positionForLatLngBounds != null) {
        return positionForLatLngBounds;
      }
    }
    return mapboxMap.getCameraPosition();
  }

  private boolean isForceUpdate() {
    if (forceUpdateZoom) {
      forceUpdateZoom = false;
      return true;
    }
    return false;
  }

  /**
   * Looks to see if we have a new step.
   *
   * @param routeProgress provides updated step information
   * @return true if new step, false if not
   */
  private boolean isNewStep(RouteProgress routeProgress) {
    boolean isNewStep = currentStep == null || !currentStep.equals(routeProgress.currentLegProgress().currentStep());
    currentStep = routeProgress.currentLegProgress().currentStep();
    resetAlertLevels(isNewStep);
    return isNewStep;
  }

  private void resetAlertLevels(boolean isNewStep) {
    if (isNewStep) {
      hasPassedLowAlertLevel = false;
      hasPassedMediumAlertLevel = false;
      hasPassedHighAlertLevel = false;
    }
  }

  private boolean validLocationAndProgress(RouteInformation routeInformation) {
    return routeInformation.location() != null && routeInformation.routeProgress() != null;
  }

  private boolean shouldUpdateZoom(RouteInformation routeInformation) {
    RouteProgress progress = routeInformation.routeProgress();
    return isForceUpdate()
      || isNewStep(progress)
      || isLowAlert(progress)
      || isMediumAlert(progress)
      || isHighAlert(progress);
  }

  private boolean isLowAlert(RouteProgress progress) {
    if (!hasPassedLowAlertLevel) {
      double durationRemaining = progress.currentLegProgress().currentStepProgress().durationRemaining();
      double stepDuration = progress.currentLegProgress().currentStep().duration();
      boolean isLowAlert = durationRemaining < NavigationConstants.NAVIGATION_LOW_ALERT_DURATION;
      boolean hasValidStepDuration = stepDuration > NavigationConstants.NAVIGATION_LOW_ALERT_DURATION;
      if (hasValidStepDuration && isLowAlert) {
        hasPassedLowAlertLevel = true;
        return true;
      }
    }
    return false;
  }

  private boolean isMediumAlert(RouteProgress progress) {
    if (!hasPassedMediumAlertLevel) {
      double durationRemaining = progress.currentLegProgress().currentStepProgress().durationRemaining();
      double stepDuration = progress.currentLegProgress().currentStep().duration();
      boolean isMediumAlert = durationRemaining < NavigationConstants.NAVIGATION_MEDIUM_ALERT_DURATION;
      boolean hasValidStepDuration = stepDuration > NavigationConstants.NAVIGATION_MEDIUM_ALERT_DURATION;
      if (hasValidStepDuration && isMediumAlert) {
        hasPassedMediumAlertLevel = true;
        return true;
      }
    }
    return false;
  }

  private boolean isHighAlert(RouteProgress progress) {
    if (!hasPassedHighAlertLevel) {
      double durationRemaining = progress.currentLegProgress().currentStepProgress().durationRemaining();
      double stepDuration = progress.currentLegProgress().currentStep().duration();
      boolean isHighAlert = durationRemaining < NavigationConstants.NAVIGATION_HIGH_ALERT_DURATION;
      boolean hasValidStepDuration = stepDuration > NavigationConstants.NAVIGATION_HIGH_ALERT_DURATION;
      if (hasValidStepDuration && isHighAlert) {
        hasPassedHighAlertLevel = true;
        return true;
      }
    }
    return false;
  }
}

package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

import static com.mapbox.services.android.navigation.ui.v5.CameraState.NOT_TRACKING;
import static com.mapbox.services.android.navigation.ui.v5.CameraState.OVERVIEW;
import static com.mapbox.services.android.navigation.ui.v5.CameraState.TRACKING;

class NavigationPresenter {

  private NavigationContract.View view;
  private boolean resumeState;
  @CameraState.Type
  private int cameraState = TRACKING;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  void updateResumeState(boolean resumeState) {
    this.resumeState = resumeState;
  }

  void onRecenterClick() {
    setTracking();
  }

  void onMapScroll() {
    if (cameraState != NOT_TRACKING) {
      setNotTracking();
    }
  }

  void onRouteOverviewClick() {
    setOverview();
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
    if (!resumeState) {
      view.updateWaynameVisibility(true);
      view.startCamera(directionsRoute);
    }
  }

  void onNavigationLocationUpdate(Location location) {
    if (resumeState) {
      view.resumeCamera(location);
      resumeState = false;
    }
    view.updateNavigationMap(location);
  }

  void onDestinationUpdate(Point point) {
    view.addMarker(point);
  }

  void onShouldRecordScreenshot() {
    view.takeScreenshot();
  }

  int getCameraState() {
    return cameraState;
  }

  void setCameraState(int cameraState) {
    switch (cameraState) {
      case TRACKING:
        setTracking();
        break;
      case NOT_TRACKING:
        setNotTracking();
        break;
      case OVERVIEW:
        setOverview();
        break;
      default:
        break;
    }
  }

  private void setOverview() {
    cameraState = CameraState.OVERVIEW;
    view.updateWaynameVisibility(false);
    view.updateCameraRouteOverview();
    view.showRecenterBtn();
  }

  private void setTracking() {
    cameraState = TRACKING;
    view.setSummaryBehaviorHideable(false);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
    view.updateWaynameVisibility(true);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  private void setNotTracking() {
    cameraState = CameraState.NOT_TRACKING;
    view.setSummaryBehaviorHideable(true);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
    view.setNotTracking();
    view.updateWaynameVisibility(false);
    view.showRecenterBtn();
  }
}

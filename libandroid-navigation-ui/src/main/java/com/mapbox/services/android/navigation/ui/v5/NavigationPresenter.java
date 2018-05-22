package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationPresenter {

  private NavigationContract.View view;
  private boolean resumeState;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  void updateResumeState(boolean resumeState) {
    this.resumeState = resumeState;
  }

  void onRecenterClick() {
    view.setSummaryBehaviorHideable(false);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
    view.updateWaynameVisibility(true);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  void onMapScroll() {
    if (!view.isSummaryBottomSheetHidden()) {
      view.setSummaryBehaviorHideable(true);
      view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
      view.updateCameraTrackingEnabled(false);
      view.updateWaynameVisibility(false);
    }
  }

  void onSummaryBottomSheetHidden() {
    if (view.isSummaryBottomSheetHidden()) {
      view.showRecenterBtn();
    }
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
    view.updateWaynameVisibility(true);
    if (!resumeState) {
      view.startCamera(directionsRoute);
    }
  }

  void onDestinationUpdate(Point point) {
    view.addMarker(point);
  }

  void onShouldRecordScreenshot() {
    view.takeScreenshot();
  }

  void onNavigationLocationUpdate(Location location) {
    if (resumeState && !view.isRecenterButtonVisible()) {
      view.resumeCamera(location);
      resumeState = false;
    }
    view.updateNavigationMap(location);
  }

  void onInstructionListVisibilityChanged(boolean visible) {
    if (visible) {
      view.hideRecenterBtn();
    } else {
      if (view.isSummaryBottomSheetHidden()) {
        view.showRecenterBtn();
      }
    }
  }

  void onRouteOverviewClick() {
    view.updateWaynameVisibility(false);
    view.updateCameraRouteOverview();
    view.showRecenterBtn();
  }
}

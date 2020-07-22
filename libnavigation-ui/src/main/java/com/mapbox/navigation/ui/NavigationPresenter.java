package com.mapbox.navigation.ui;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.ui.internal.NavigationContract;

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
    view.setWayNameActive(true);
    if (!TextUtils.isEmpty(view.retrieveWayNameText())) {
      view.setWayNameVisibility(true);
    }
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  void onCameraTrackingDismissed() {
    if (!view.isSummaryBottomSheetHidden()) {
      view.setSummaryBehaviorHideable(true);
      view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
      view.setWayNameActive(false);
      view.setWayNameVisibility(false);
    }
  }

  void onSummaryBottomSheetHidden() {
    if (view.isSummaryBottomSheetHidden()) {
      view.showRecenterBtn();
    }
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
    if (resumeState && view.isRecenterButtonVisible()) {
      view.updateCameraRouteOverview();
    } else {
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
  }

  void onWayNameChanged(@NonNull String wayName) {
    if (TextUtils.isEmpty(wayName) || view.isSummaryBottomSheetHidden()) {
      view.setWayNameActive(false);
      view.setWayNameVisibility(false);
      return;
    }
    view.updateWayNameView(wayName);
    view.setWayNameActive(true);
    view.setWayNameVisibility(true);
  }

  void onNavigationStopped() {
    view.setWayNameActive(false);
    view.setWayNameVisibility(false);
  }

  void onRouteOverviewClick() {
    view.setWayNameActive(false);
    view.setWayNameVisibility(false);
    view.updateCameraRouteOverview();
    view.showRecenterBtn();
  }

  void onFeedbackSent() {
    view.onFeedbackSent();
  }

  void onGuidanceViewChange(int left, int top, int width, int height) {
    view.onGuidanceViewChange(left, top, width, height);
  }
}
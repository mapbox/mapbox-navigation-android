package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationPresenter {

  private NavigationContract.View view;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  void onRecenterClick() {
    view.setSummaryBehaviorHideable(false);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  void onCancelBtnClick() {
    view.finishNavigationView();
  }

  void onMapScroll() {
    view.setSummaryBehaviorHideable(true);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
    view.setCameraTrackingEnabled(false);
  }

  void onSummaryBottomSheetHidden() {
    view.showRecenterBtn();
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
    view.startCamera(directionsRoute);
  }

  void onDestinationUpdate(Point point) {
    view.addMarker(point);
  }

  void onShouldRecordScreenshot() {
    view.takeScreenshot();
  }

  void onNavigationLocationUpdate(Location location) {
    view.resumeCamera(location);
    view.updateLocationLayer(location);
  }
}

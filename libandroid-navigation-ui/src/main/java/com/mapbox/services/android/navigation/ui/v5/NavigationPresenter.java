package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationPresenter {

  private NavigationContract.View view;
  private int bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  // in case of you need to to something after map init
  void onMapReady() {
  }

  @UiThread
  void resume() {
  }

  @UiThread
  void pause() {
  }

  void onRecenterClick() {
    view.setSummaryBehaviorHideable(false);
    bottomSheetState = BottomSheetBehavior.STATE_EXPANDED;
    view.setSummaryBehaviorState(bottomSheetState);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  void onCancelBtnClick() {
    view.finishNavigationView();
  }

  void onMapScroll() {
    if (bottomSheetState != BottomSheetBehavior.STATE_HIDDEN) {
      view.setSummaryBehaviorHideable(true);
      bottomSheetState = BottomSheetBehavior.STATE_HIDDEN;
      view.setSummaryBehaviorState(bottomSheetState);
      view.setCameraTrackingEnabled(false);
    }
  }

  void onSummaryBottomSheetHidden() {
    view.showRecenterBtn();
  }

  void retrieveBottomSheetState(int bottomSheetState) {
    this.bottomSheetState = bottomSheetState;
    boolean isShowing = bottomSheetState == BottomSheetBehavior.STATE_EXPANDED;
    view.setSummaryBehaviorHideable(!isShowing);
    view.setSummaryBehaviorState(bottomSheetState);
  }

  void retrieveRecenterBtn(boolean isRecentBtnVisible) {
    if (isRecentBtnVisible) {
      view.showRecenterBtn();
    } else {
      view.hideRecenterBtn();
    }
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

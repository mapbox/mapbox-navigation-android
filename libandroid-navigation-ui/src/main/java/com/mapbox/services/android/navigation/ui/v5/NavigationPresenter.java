package com.mapbox.services.android.navigation.ui.v5;

import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationPresenter {

  private NavigationContract.View view;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  void onMuteClick(boolean isMuted) {
    view.setMuted(isMuted);
  }

  void onRecenterClick() {
    view.setSummaryBehaviorHideable(false);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_COLLAPSED);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  void onExpandArrowClick(int summaryBehaviorState) {
    view.setSummaryBehaviorState(summaryBehaviorState == BottomSheetBehavior.STATE_COLLAPSED
      ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
  }

  void onCancelBtnClick() {
    view.finishNavigationView();
  }

  void onDirectionsOptionClick() {
    view.setSheetShadowVisibility(false);
    view.setSummaryOptionsVisibility(false);
    view.setSummaryDirectionsVisibility(true);
  }

  void onMapScroll() {
    view.setSummaryBehaviorHideable(true);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
    view.setCameraTrackingEnabled(false);
  }

  void onSummaryBottomSheetExpanded() {
    view.setCancelBtnClickable(false);

    if (view.isSummaryDirectionsVisible()) {
      view.setSheetShadowVisibility(false);
    } else {
      view.setMapPadding(0, 0, 0, view.getBottomSheetHeight());
    }
  }

  void onSummaryBottomSheetCollapsed() {
    view.setCancelBtnClickable(true);
    view.setSummaryOptionsVisibility(true);
    view.setSummaryDirectionsVisibility(false);

    int bottomPadding = view.getMapPadding()[3];
    if (bottomPadding != view.getBottomSheetPeekHeight()) {
      view.setMapPadding(0, 0, 0, view.getBottomSheetPeekHeight());
    }
  }

  void onSummaryBottomSheetHidden() {
    view.showRecenterBtn();
  }

  void onBottomSheetSlide(float slideOffset, boolean sheetShadowVisible) {
    if (slideOffset < 1f && !sheetShadowVisible) {
      view.setSheetShadowVisibility(true);
    }
    if (view.isSummaryDirectionsVisible()) {
      view.animateInstructionViewAlpha(1 - slideOffset);
    }
    view.animateCancelBtnAlpha(1 - slideOffset);
    view.animateExpandArrowRotation(180 * slideOffset);
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
  }

  void onDestinationUpdate(Point point) {
    view.addMarker(point);
  }

  void onNavigationRunning() {
    view.showInstructionView();
  }
}

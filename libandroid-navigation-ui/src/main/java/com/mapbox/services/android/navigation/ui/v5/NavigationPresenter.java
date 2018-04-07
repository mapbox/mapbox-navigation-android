package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationPresenter {

  private static final int RECENTER_MAX_TIME = 15000;
  private static final Object lock = new Object();
  private final Handler recenterHandler = new Handler();
  private final Runnable recenterRunnable;

  private NavigationContract.View view;
  private int bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED;
  private int millisToRecenter = RECENTER_MAX_TIME;
  private long lastScrollTimestamp = -1;
  private boolean timerIsRunning = false;
  private boolean isViewAttached = false;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
    recenterRunnable = new Runnable() {
      @Override
      public void run() {
        synchronized (lock) {
          if (isViewAttached) {
            if (lastScrollTimestamp != -1 && System.currentTimeMillis() - lastScrollTimestamp > millisToRecenter) {
              lastScrollTimestamp = -1;
              millisToRecenter = RECENTER_MAX_TIME;
              timerIsRunning = false;
              onRecenterClick();
              return;
            }
            recenterHandler.postDelayed(recenterRunnable, 1000);
          }
        }
      }
    };
  }

  // in case of you need to to something after map init
  void onMapReady() {
  }

  @UiThread
  void resume() {
    isViewAttached = true;
    if (timerIsRunning) {
      recenterHandler.postDelayed(recenterRunnable, 1000);
    }
  }

  @UiThread
  void pause() {
    synchronized (lock) {
      isViewAttached = false;
      //stop handler when view not visible
      recenterHandler.removeCallbacks(recenterRunnable);
    }
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
    lastScrollTimestamp = System.currentTimeMillis();
    startRecenterTimer();

    if (bottomSheetState != BottomSheetBehavior.STATE_HIDDEN) {
      view.setSummaryBehaviorHideable(true);
      bottomSheetState = BottomSheetBehavior.STATE_HIDDEN;
      view.setSummaryBehaviorState(bottomSheetState);
      view.setCameraTrackingEnabled(false);
    }
  }

  void onSummaryBottomSheetHidden() {
    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) {
      view.showRecenterBtn();
    }
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

  @UiThread
  private void startRecenterTimer() {
    synchronized (lock) {
      if (!timerIsRunning) {
        timerIsRunning = true;
        recenterHandler.postDelayed(recenterRunnable, 1000);
      }
    }
  }
}

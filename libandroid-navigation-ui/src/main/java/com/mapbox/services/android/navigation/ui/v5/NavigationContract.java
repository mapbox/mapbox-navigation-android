package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;

public interface NavigationContract {

  interface View {

    void setSummaryBehaviorState(int state);

    void setSummaryBehaviorHideable(boolean isHideable);

    boolean isSummaryBottomSheetHidden();

    void updateWaynameVisibility(boolean isVisible);

    void updateCameraTrackingMode(@NavigationCamera.TrackingMode int trackingMode);

    void resetCameraPosition();

    void showRecenterBtn();

    void hideRecenterBtn();

    void drawRoute(DirectionsRoute directionsRoute);

    void addMarker(Point point);

    void takeScreenshot();

    void startCamera(DirectionsRoute directionsRoute);

    void resumeCamera(Location location);

    void updateNavigationMap(Location location);

    boolean isRecenterButtonVisible();

    void updateCameraRouteOverview();
  }
}

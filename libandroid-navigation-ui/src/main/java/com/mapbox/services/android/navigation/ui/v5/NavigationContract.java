package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

public interface NavigationContract {

  interface View {

    void setSummaryBehaviorState(int state);

    void setSummaryBehaviorHideable(boolean isHideable);

    void setCameraTrackingEnabled(boolean isEnabled);

    void resetCameraPosition();

    void showRecenterBtn();

    void hideRecenterBtn();

    void drawRoute(DirectionsRoute directionsRoute);

    void addMarker(Point point);

    void finishNavigationView();

    void takeScreenshot();

    void startCamera(DirectionsRoute directionsRoute);

    void resumeCamera(Location location);

    void updateLocationLayer(Location location);
  }
}

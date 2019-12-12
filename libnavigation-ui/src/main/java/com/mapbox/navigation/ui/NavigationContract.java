package com.mapbox.navigation.ui;

import android.location.Location;

import androidx.annotation.NonNull;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.geojson.Point;

public interface NavigationContract {

  interface View {

    void setSummaryBehaviorState(int state);

    void setSummaryBehaviorHideable(boolean isHideable);

    boolean isSummaryBottomSheetHidden();

    void updateWayNameVisibility(boolean isVisible);

    void updateWayNameView(@NonNull String wayName);

    void resetCameraPosition();

    void showRecenterBtn();

    void hideRecenterBtn();

    void drawRoute(Route route);

    void addMarker(Point point);

    void takeScreenshot();

    void startCamera(Route route);

    void resumeCamera(Location location);

    void updateNavigationMap(Location location);

    boolean isRecenterButtonVisible();

    void updateCameraRouteOverview();
  }
}

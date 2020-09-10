package com.mapbox.navigation.ui.internal;

import android.location.Location;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet;
import com.mapbox.navigation.ui.feedback.FeedbackItem;

public interface NavigationContract {

  interface View {

    void setSummaryBehaviorState(int state);

    void setSummaryBehaviorHideable(boolean isHideable);

    boolean isSummaryBottomSheetHidden();

    void setWayNameActive(boolean isActive);

    void setWayNameVisibility(boolean isVisible);

    String retrieveWayNameText();

    void updateWayNameView(@NonNull String wayName);

    void resetCameraPosition();

    void showRecenterBtn();

    void hideRecenterBtn();

    void drawRoute(DirectionsRoute directionsRoute);

    void addMarker(Point point);

    void takeScreenshot();

    void startCamera(DirectionsRoute directionsRoute);

    void resumeCamera(Location location);

    boolean isRecenterButtonVisible();

    /**
     * @deprecated use {@link #updateCameraRouteGeometryOverview()}
     */
    @Deprecated
    void updateCameraRouteOverview();

    boolean updateCameraRouteGeometryOverview();

    void onFeedbackFlowStatusChanged(@FeedbackBottomSheet.FeedbackFlowStatus int status);

    void onFinalDestinationArrival();

    void onGuidanceViewChange(int left, int top, int width, int height);
  }
}

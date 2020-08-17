package com.mapbox.navigation.ui;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.navigation.ui.internal.utils.ViewUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Used to take a snapshot of the current map when sending the feedback event.
 */
class NavigationSnapshotReadyCallback implements MapboxMap.SnapshotReadyCallback {

  private NavigationView navigationView;
  private NavigationViewModel navigationViewModel;

  NavigationSnapshotReadyCallback(NavigationView navigationView, NavigationViewModel navigationViewModel) {
    this.navigationView = navigationView;
    this.navigationViewModel = navigationViewModel;
  }

  @Override
  public void onSnapshotReady(@NotNull Bitmap snapshot) {
    ImageView screenshotView = updateScreenshotViewWithSnapshot(snapshot);
    updateFeedbackScreenshot();
    resetViewVisibility(screenshotView);
  }

  @NonNull
  private ImageView updateScreenshotViewWithSnapshot(Bitmap snapshot) {
    ImageView screenshotView = navigationView.findViewById(R.id.screenshotView);
    screenshotView.setVisibility(View.VISIBLE);
    screenshotView.setImageBitmap(snapshot);
    return screenshotView;
  }

  private void updateFeedbackScreenshot() {
    MapView mapView = navigationView.findViewById(R.id.navigationMapView);
    mapView.setVisibility(View.INVISIBLE);
    Bitmap capture = ViewUtils.captureView(mapView);
    String encoded = ViewUtils.encodeView(capture);
    navigationViewModel.updateFeedbackScreenshot(encoded);
  }

  private void resetViewVisibility(@NonNull ImageView screenshotView) {
    screenshotView.setVisibility(View.INVISIBLE);
    MapView mapView = navigationView.findViewById(R.id.navigationMapView);
    mapView.setVisibility(View.VISIBLE);
  }
}

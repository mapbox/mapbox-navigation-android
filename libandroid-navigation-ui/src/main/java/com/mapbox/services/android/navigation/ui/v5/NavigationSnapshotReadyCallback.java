package com.mapbox.services.android.navigation.ui.v5;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.utils.ViewUtils;

public class NavigationSnapshotReadyCallback implements MapboxMap.SnapshotReadyCallback {

  private NavigationView navigationView;
  private NavigationViewModel navigationViewModel;

  NavigationSnapshotReadyCallback(NavigationView navigationView, NavigationViewModel navigationViewModel) {
    this.navigationView = navigationView;
    this.navigationViewModel = navigationViewModel;
  }

  @Override
  public void onSnapshotReady(Bitmap snapshot) {
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

  private void resetViewVisibility(ImageView screenshotView) {
    screenshotView.setVisibility(View.INVISIBLE);
    MapView mapView = navigationView.findViewById(R.id.navigationMapView);
    mapView.setVisibility(View.VISIBLE);
  }
}

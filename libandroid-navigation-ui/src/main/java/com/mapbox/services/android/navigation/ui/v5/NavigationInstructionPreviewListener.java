package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsPreviewListener;

import java.util.List;

public class NavigationInstructionPreviewListener implements BannerInstructionsPreviewListener {
  private NavigationPresenter navigationPresenter;

  public NavigationInstructionPreviewListener(NavigationPresenter navigationPresenter) {
    this.navigationPresenter = navigationPresenter;
  }

  @Override
  public void willStartPreview(LegStep step, List<Point> currentPoints, List<Point> upcomingPoints) {
    navigationPresenter.moveCameraTo(step, currentPoints, upcomingPoints);
  }

  @Override
  public void willStopPreview() {
    navigationPresenter.onResetCameraAndArrow();
  }
}

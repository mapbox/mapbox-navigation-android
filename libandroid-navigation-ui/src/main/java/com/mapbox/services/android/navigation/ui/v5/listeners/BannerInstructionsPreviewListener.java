package com.mapbox.services.android.navigation.ui.v5.listeners;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;

import java.util.List;

public interface BannerInstructionsPreviewListener {
  void willStartPreview(LegStep step, List<Point> currentPoints, List<Point> upcomingPoints);

  void willStopPreview();
}

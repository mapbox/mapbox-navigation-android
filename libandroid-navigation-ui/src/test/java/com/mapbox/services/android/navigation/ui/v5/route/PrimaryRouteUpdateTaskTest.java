package com.mapbox.services.android.navigation.ui.v5.route;

import com.mapbox.geojson.FeatureCollection;

import org.junit.Test;

import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class PrimaryRouteUpdateTaskTest {

  @Test
  public void doInBackground_returnsEmptyCollection() {
    int primaryRouteIndex = 0;
    List<FeatureCollection> routeFeatureCollections = Collections.emptyList();
    OnPrimaryRouteUpdatedCallback callback = mock(OnPrimaryRouteUpdatedCallback.class);
    PrimaryRouteUpdateTask task = new PrimaryRouteUpdateTask(primaryRouteIndex, routeFeatureCollections, callback);

    List<FeatureCollection> updatedCollection = task.doInBackground(mock(Void.class));

    assertEquals(routeFeatureCollections, updatedCollection);
  }
}
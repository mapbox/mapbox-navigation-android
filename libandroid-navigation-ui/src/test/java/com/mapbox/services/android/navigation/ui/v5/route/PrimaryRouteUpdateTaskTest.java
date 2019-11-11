package com.mapbox.services.android.navigation.ui.v5.route;

import android.os.Handler;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrimaryRouteUpdateTaskTest {

  @Test
  public void run_onPrimaryRouteUpdatedIsCalled() {
    int primaryRouteIndex = 0;
    FeatureCollection mockedFeatureCollection = mock(FeatureCollection.class);
    List<FeatureCollection> routeFeatureCollections = new ArrayList<>();
    List<Feature> mockedFeatures = new ArrayList<>();
    Feature mockedFeature = mock(Feature.class);
    mockedFeatures.add(mockedFeature);
    when(mockedFeatureCollection.features()).thenReturn(mockedFeatures);
    routeFeatureCollections.add(mockedFeatureCollection);
    OnPrimaryRouteUpdatedCallback callback = mock(OnPrimaryRouteUpdatedCallback.class);
    Handler mockedHandler = mock(Handler.class);
    ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
    PrimaryRouteUpdateTask task = new PrimaryRouteUpdateTask(primaryRouteIndex, routeFeatureCollections, callback, mockedHandler);

    task.run();

    verify(mockedHandler).post(runnable.capture());
    runnable.getValue().run();
    verify(callback).onPrimaryRouteUpdated(eq(routeFeatureCollections));
  }
}
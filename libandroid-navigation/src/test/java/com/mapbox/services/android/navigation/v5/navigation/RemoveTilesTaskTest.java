package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.Navigator;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RemoveTilesTaskTest {

  @Test
  public void checksOnRemoveIsCalledWhenTilesAreRemoved() {
    Navigator mockedNavigator = mock(Navigator.class);
    String aTilePath = "/some/path/version";
    Point southwest = Point.fromLngLat(1.0, 2.0);
    Point northeast = Point.fromLngLat(3.0, 4.0);
    OnOfflineTilesRemovedCallback mockedCallback = mock(OnOfflineTilesRemovedCallback.class);
    RemoveTilesTask theRemoveTilesTask = new RemoveTilesTask(mockedNavigator, aTilePath, southwest,
      northeast, mockedCallback);

    theRemoveTilesTask.onPostExecute(9L);

    verify(mockedCallback).onRemoved(eq(9L));
  }
}
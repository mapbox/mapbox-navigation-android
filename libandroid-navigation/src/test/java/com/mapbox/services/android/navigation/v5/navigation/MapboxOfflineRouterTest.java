package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Point;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MapboxOfflineRouterTest {

  @Test
  public void initializeOfflineData_filePathIncludesVersion() {
    String tilePath = "/some/path/";
    OfflineNavigator offlineNavigator = mock(OfflineNavigator.class);
    OnOfflineTilesConfiguredCallback callback = mock(OnOfflineTilesConfiguredCallback.class);
    MapboxOfflineRouter offlineRouter = buildRouter(tilePath, offlineNavigator);

    offlineRouter.configure("version", callback);

    verify(offlineNavigator).configure("/some/path/version", callback);
  }

  @Test
  public void findOfflineRoute_offlineNavigatorIsCalled() {
    OfflineNavigator offlineNavigator = mock(OfflineNavigator.class);
    OfflineRoute offlineRoute = mock(OfflineRoute.class);
    OnOfflineRouteFoundCallback callback = mock(OnOfflineRouteFoundCallback.class);
    MapboxOfflineRouter offlineRouter = buildRouter(offlineNavigator);

    offlineRouter.findRoute(offlineRoute, callback);

    verify(offlineNavigator).retrieveRouteFor(offlineRoute, callback);
  }

  @Test
  public void fetchAvailableTileVersions() {
    String accessToken = "access_token";
    OnTileVersionsFoundCallback callback = mock(OnTileVersionsFoundCallback.class);
    OfflineTileVersions offlineTileVersions = mock(OfflineTileVersions.class);
    MapboxOfflineRouter offlineRouter = buildRouter(offlineTileVersions);

    offlineRouter.fetchAvailableTileVersions(accessToken, callback);

    verify(offlineTileVersions).fetchRouteTileVersions(accessToken, callback);
  }

  @Test
  public void checksRemoveTiles() {
    String aTilePath = "/some/path/";
    OfflineNavigator anOfflineNavigator = mock(OfflineNavigator.class);
    MapboxOfflineRouter theOfflineRouter = buildRouter(aTilePath, anOfflineNavigator);
    Point southwest = Point.fromLngLat(1.0, 2.0);
    Point northeast = Point.fromLngLat(3.0, 4.0);
    BoundingBox aBoundingBox = BoundingBox.fromPoints(southwest, northeast);
    OnOfflineTilesRemovedCallback aCallback = mock(OnOfflineTilesRemovedCallback.class);

    theOfflineRouter.removeTiles("a_version", aBoundingBox, aCallback);

    verify(anOfflineNavigator).removeTiles(eq("/some/path/a_version"), eq(southwest), eq(northeast),
      eq(aCallback));
  }

  private MapboxOfflineRouter buildRouter(String tilePath, OfflineNavigator offlineNavigator) {
    return new MapboxOfflineRouter(tilePath, offlineNavigator, mock(OfflineTileVersions.class));
  }

  private MapboxOfflineRouter buildRouter(OfflineNavigator offlineNavigator) {
    return new MapboxOfflineRouter("", offlineNavigator, mock(OfflineTileVersions.class));
  }

  private MapboxOfflineRouter buildRouter(OfflineTileVersions offlineTileVersions) {
    return new MapboxOfflineRouter("", mock(OfflineNavigator.class), offlineTileVersions);
  }
}
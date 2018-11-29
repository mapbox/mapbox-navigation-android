package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

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
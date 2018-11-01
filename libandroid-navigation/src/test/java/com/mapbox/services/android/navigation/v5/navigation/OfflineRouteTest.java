package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OfflineRouteTest {

  @Test
  public void addBicycleTypeIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .bicycleType(OfflineCriteria.ROAD).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("bicycle_type=Road"));
  }

  @Test(expected = ServicesException.class)
  public void checksBicycleTypeRequired() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute.Builder offlineRouteBuilder = OfflineRoute.builder(onlineBuilder)
      .bicycleType("not_a_valid_bicycle_type");

    offlineRouteBuilder.build();
  }

  @Test(expected = ServicesException.class)
  public void checksProperWaypointTypesRequired() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    List<String> invalidWaypointTypes = new ArrayList<>();
    invalidWaypointTypes.add("invalid");
    invalidWaypointTypes.add("waypoint");
    invalidWaypointTypes.add("types");
    OfflineRoute.Builder offlineRouteBuilder = OfflineRoute.builder(onlineBuilder)
      .waypointTypes(invalidWaypointTypes);

    offlineRouteBuilder.build();
  }

  @Test
  public void addCyclingSpeedIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .cyclingSpeed(10.0f).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("cycling_speed=10.0"));
  }

  @Test
  public void addUseRoadsIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .useRoads(0.5f).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("use_roads=0.5"));
  }

  @Test
  public void addUseHillsIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .useHills(0.5f).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("use_hills=0.5"));
  }

  @Test
  public void addUseFerryIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .useFerry(0.5f).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("use_ferry=0.5"));
  }

  @Test
  public void addAvoidBadSurfacesIncludedInRequest() {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .avoidBadSurfaces(0.5f).build();

    String offlineUrl = offlineRoute.buildUrl();

    assertTrue(offlineUrl.contains("avoid_bad_surfaces=0.5"));
  }

  @Test
  public void addWaypointTypesIncludedInRequest() throws UnsupportedEncodingException {
    NavigationRoute.Builder onlineBuilder = provideOnlineRouteBuilder();
    List<String> waypointTypes = new ArrayList<>();
    waypointTypes.add(OfflineCriteria.BREAK);
    waypointTypes.add(OfflineCriteria.THROUGH);
    waypointTypes.add(OfflineCriteria.BREAK);
    OfflineRoute offlineRoute = OfflineRoute.builder(onlineBuilder)
      .waypointTypes(waypointTypes).build();

    String offlineUrl = offlineRoute.buildUrl();
    String offlineUrlDecoded = URLDecoder.decode(offlineUrl, "UTF-8");

    assertTrue(offlineUrlDecoded.contains("break;through;break"));
  }

  private NavigationRoute.Builder provideOnlineRouteBuilder() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute.Builder onlineBuilder = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .addWaypoint(Point.fromLngLat(3.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING);
    return onlineBuilder;
  }
}
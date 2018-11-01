package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
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

public class OfflineNavigationRouteTest {

  @Test(expected = IllegalArgumentException.class)
  public void checksNonNullNavigationRouteRequired() {
    NavigationRoute nullRoute = null;
    OfflineNavigationOptions anyOfflineOptions = mock(OfflineNavigationOptions.class);

    OfflineNavigationRoute.buildUrl(nullRoute, anyOfflineOptions);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checksNonNullOfflineNavigationOptionsRequired() {
    NavigationRoute route = mock(NavigationRoute.class);
    OfflineNavigationOptions nullOfflineOptions = null;

    OfflineNavigationRoute.buildUrl(route, nullOfflineOptions);
  }

  @Test
  public void addBicycleTypeIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .bicycleType(OfflineCriteria.ROAD).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("bicycle_type=Road"));
  }

  @Test
  public void addCyclingSpeedIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .cyclingSpeed(10.0f).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("cycling_speed=10.0"));
  }

  @Test
  public void addUseRoadsIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .useRoads(0.5f).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("use_roads=0.5"));
  }

  @Test
  public void addUseHillsIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .useHills(0.5f).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("use_hills=0.5"));
  }

  @Test
  public void addUseFerryIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .useFerry(0.5f).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("use_ferry=0.5"));
  }

  @Test
  public void addAvoidBadSurfacesIncludedInRequest() {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .avoidBadSurfaces(0.5f).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);

    assertTrue(offlineUrl.contains("avoid_bad_surfaces=0.5"));
  }

  @Test
  public void addWaypointTypesIncludedInRequest() throws UnsupportedEncodingException {
    Context context = mock(Context.class);
    LocaleUtils localeUtils = mock(LocaleUtils.class);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
    NavigationRoute route = NavigationRoute.builder(context, localeUtils)
      .accessToken("pk.XXX")
      .origin(Point.fromLngLat(1.0, 2.0))
      .addWaypoint(Point.fromLngLat(4.0, 3.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();
    List<String> waypointTypes = new ArrayList<>();
    waypointTypes.add(OfflineCriteria.BREAK);
    waypointTypes.add(OfflineCriteria.THROUGH);
    waypointTypes.add(OfflineCriteria.BREAK);
    OfflineNavigationOptions offlineOptions = OfflineNavigationOptions.builder()
      .waypointTypes(waypointTypes).build();

    String offlineUrl = OfflineNavigationRoute.buildUrl(route, offlineOptions);
    String offlineUrlDecoded = URLDecoder.decode(offlineUrl, "UTF-8");

    assertTrue(offlineUrlDecoded.contains("break;through;break"));
  }

}
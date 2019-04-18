package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MapboxNavigationNotificationTest extends BaseTest {

  private static final String DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json";
  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    final String json = loadJsonFixture(DIRECTIONS_ROUTE_FIXTURE);
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    DirectionsResponse response = gson.fromJson(json, DirectionsResponse.class);
    route = response.routes().get(0);
  }

  @Test
  public void checksArrivalTime() throws Exception {
    MapboxNavigation mockedMapboxNavigation = createMapboxNavigation();
    Context mockedContext = createContext();
    Notification mockedNotification = mock(Notification.class);
    MapboxNavigationNotification mapboxNavigationNotification = new MapboxNavigationNotification(mockedContext,
      mockedMapboxNavigation, mockedNotification);
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Calendar mockedTime = Calendar.getInstance();
    mockedTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    long aprilFifteenThreeFourtyFourFiftyThreePmTwoThousandNineteen = 1555357493308L;
    mockedTime.setTimeInMillis(aprilFifteenThreeFourtyFourFiftyThreePmTwoThousandNineteen);

    String formattedArrivalTime = mapboxNavigationNotification.generateArrivalTime(routeProgress, mockedTime);

    assertEquals("8:46 pm ETA", formattedArrivalTime);
  }

  private MapboxNavigation createMapboxNavigation() {
    MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
    when(mockedMapboxNavigation.getRoute()).thenReturn(route);
    MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
    when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
    when(mockedMapboxNavigationOptions.roundingIncrement()).thenReturn(NavigationConstants.ROUNDING_INCREMENT_FIVE);
    return mockedMapboxNavigation;
  }

  private Context createContext() {
    Context mockedContext = mock(Context.class);
    Configuration mockedConfiguration = new Configuration();
    mockedConfiguration.locale = new Locale("en");
    Resources mockedResources = mock(Resources.class);
    when(mockedContext.getResources()).thenReturn(mockedResources);
    when(mockedResources.getConfiguration()).thenReturn(mockedConfiguration);
    PackageManager mockedPackageManager = mock(PackageManager.class);
    when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
    when(mockedContext.getString(anyInt())).thenReturn("%s ETA");
    return mockedContext;
  }
}

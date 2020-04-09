package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("KotlinInternalInJava")
@RunWith(RobolectricTestRunner.class)
public class MapboxNavigationNotificationTest extends BaseTest {

  private static final String DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json";
  private static final String BANNER_TEXT_YOU_WILL_ARRIVE = "You will arrive";
  private static final String BANNER_TEXT_YOU_HAVE_ARRIVED = "You have arrived";

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
    MapboxNavigationNotification mapboxNavigationNotification = createMapboxNavigationNotification();

    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Calendar mockedTime = Calendar.getInstance();
    mockedTime.setTimeZone(TimeZone.getTimeZone("UTC"));
    long aprilFifteenThreeFortyFourFiftyThreePmTwoThousandNineteen = 1555357493308L;
    mockedTime.setTimeInMillis(aprilFifteenThreeFortyFourFiftyThreePmTwoThousandNineteen);

    String formattedArrivalTime = mapboxNavigationNotification.generateArrivalTime(routeProgress, mockedTime);

    assertEquals("8:46 pm ETA", formattedArrivalTime);
  }

  @Test
  public void checksInstructionTextNotUpdatedIfRouteProgressBannerInstructionIsNull() throws Exception {
    MapboxNavigationNotification mapboxNavigationNotification = createMapboxNavigationNotification();

    RouteProgress routeProgress = buildDefaultTestRouteProgress();

    mapboxNavigationNotification.updateNotificationViews(routeProgress);

    assertNull(mapboxNavigationNotification.retrieveInstructionText());
  }

  @Test
  public void checksInstructionTextIsUpdatedIfInstructionTextIsNotInitialized() throws Exception {
    MapboxNavigationNotification mapboxNavigationNotification = createMapboxNavigationNotification();

    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerText aBannerSection = BannerText.builder()
            .text(BANNER_TEXT_YOU_HAVE_ARRIVED)
            .build();
    BannerInstructions aBannerInstruction = BannerInstructions.builder()
            .primary(aBannerSection)
            .distanceAlongGeometry(0.1f)
            .build();
    routeProgress = routeProgress.toBuilder().bannerInstruction(aBannerInstruction).build();

    mapboxNavigationNotification.updateNotificationViews(routeProgress);

    assertEquals(BANNER_TEXT_YOU_HAVE_ARRIVED, mapboxNavigationNotification.retrieveInstructionText());
  }

  @Test
  public void checksInstructionTextIsUpdatedIfInstructionTextIsInitializedAndManeuverIsDifferent() throws Exception {
    MapboxNavigationNotification mapboxNavigationNotification = createMapboxNavigationNotification();

    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    BannerText willArriveBannerSection = BannerText.builder()
            .text(BANNER_TEXT_YOU_WILL_ARRIVE)
            .build();
    BannerInstructions willArriveBannerInstruction = BannerInstructions.builder()
            .primary(willArriveBannerSection)
            .distanceAlongGeometry(0.1f)
            .build();
    routeProgress = routeProgress.toBuilder().bannerInstruction(willArriveBannerInstruction).build();
    mapboxNavigationNotification.updateNotificationViews(routeProgress);
    assertEquals(BANNER_TEXT_YOU_WILL_ARRIVE, mapboxNavigationNotification.retrieveInstructionText());
    BannerText haveArrivedBannerSection = BannerText.builder()
            .text(BANNER_TEXT_YOU_HAVE_ARRIVED)
            .build();
    BannerInstructions haveArrivedBannerInstruction = BannerInstructions.builder()
            .primary(haveArrivedBannerSection)
            .distanceAlongGeometry(0.1f)
            .build();
    routeProgress = routeProgress.toBuilder().bannerInstruction(haveArrivedBannerInstruction).build();

    mapboxNavigationNotification.updateNotificationViews(routeProgress);

    assertEquals(BANNER_TEXT_YOU_HAVE_ARRIVED, mapboxNavigationNotification.retrieveInstructionText());
  }

  private MapboxNavigationNotification createMapboxNavigationNotification() {
    MapboxNavigation mockedMapboxNavigation = createMapboxNavigation();
    Context mockedContext = createContext();
    Notification mockedNotification = mock(Notification.class);

    return new MapboxNavigationNotification(mockedContext,
            mockedMapboxNavigation, mockedNotification);
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
    when(mockedContext.getString(anyInt())).thenReturn("%s ETA");

    Resources mockedResources = mock(Resources.class);
    when(mockedContext.getResources()).thenReturn(mockedResources);

    Configuration mockedConfiguration = new Configuration();
    mockedConfiguration.locale = new Locale("en");
    when(mockedResources.getConfiguration()).thenReturn(mockedConfiguration);

    PackageManager mockedPackageManager = mock(PackageManager.class);
    when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);

    return mockedContext;
  }
}

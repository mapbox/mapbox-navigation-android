package com.mapbox.services.android.navigation.v5.navigation;

import android.app.NotificationManager;
import android.content.Context;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NavigationNotificationTest extends BaseTest {

  private static final String DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json";

  @Mock
  NotificationManager notificationManager;

  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    final String json = loadJsonFixture(DIRECTIONS_ROUTE_FIXTURE);
    Gson gson = new Gson();
    DirectionsResponse response = gson.fromJson(json, DirectionsResponse.class);
    route = response.getRoutes().get(0);
  }

  @Test
  public void sanity() throws Exception {
    NavigationNotification navigationNotification = new NavigationNotification(
      Mockito.mock(Context.class), Mockito.mock(MapboxNavigation.class));
    Assert.assertNotNull(navigationNotification);
  }

  @Test
  public void updateDefaultNotification_onlyUpdatesNameWhenNew() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    NavigationNotification navigationNotification = new NavigationNotification(
      Mockito.mock(Context.class), Mockito.mock(MapboxNavigation.class));

    navigationNotification.updateDefaultNotification(routeProgress);
    notificationManager.getActiveNotifications()[0].getNotification().contentView.
//    verify(notificationManager, times(1)).getActiveNotifications()[0];

  }







//
//  private boolean newStepName(RouteProgress routeProgress) {
//    return currentStepName != null
//      && !currentStepName.contentEquals(routeProgress.currentLegProgress().currentStep().getName());
//  }
}

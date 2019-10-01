package com.mapbox.services.android.navigation.v5.route;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Callback;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class RouteFetcherTest {

  @Test
  public void cancelRouteCall_cancelsWithNonNullNavigationRoute() {
    Context context = mock(Context.class);
    NavigationRoute navigationRoute = mock(NavigationRoute.class);
    RouteFetcher routeFetcher = new RouteFetcher(context, "pk.xx", navigationRoute);

    routeFetcher.cancelRouteCall();

    verify(navigationRoute).cancelCall();
  }

  @Test
  public void buildRequestFrom_returnsValidBuilder() {
    Context context = buildMockContext();
    Location location = buildMockLocation();
    List<Point> remainingCoordinates = buildCoordinateList();
    RouteProgress routeProgress = buildMockProgress(remainingCoordinates);
    RouteUtils routeUtils = mock(RouteUtils.class);
    when(routeUtils.calculateRemainingWaypoints(eq(routeProgress))).thenReturn(remainingCoordinates);
    RouteFetcher routeFetcher = new RouteFetcher(context, "pk.xx", routeUtils);

    NavigationRoute.Builder builder = routeFetcher.buildRequestFrom(location, routeProgress);

    assertNotNull(builder);
  }

  @Test
  public void findRouteWith_callNavigationRoute() {
    Context context = mock(Context.class);
    NavigationRoute navigationRoute = mock(NavigationRoute.class);
    NavigationRoute.Builder builder = mock(NavigationRoute.Builder.class);
    when(builder.build()).thenReturn(navigationRoute);
    RouteFetcher routeFetcher = new RouteFetcher(context, "pk.xx", navigationRoute);

    routeFetcher.findRouteWith(builder);

    verify(navigationRoute).getRoute(any(Callback.class));
  }

  @NotNull
  private Context buildMockContext() {
    Context context = mock(Context.class);
    Resources resources = mock(Resources.class);
    Configuration configuration = new Configuration();
    configuration.setLocale(Locale.US);
    when(resources.getConfiguration()).thenReturn(configuration);
    when(context.getResources()).thenReturn(resources);
    return context;
  }

  @NotNull
  private Location buildMockLocation() {
    Location location = mock(Location.class);
    when(location.getLongitude()).thenReturn(1.23);
    when(location.getLatitude()).thenReturn(2.34);
    return location;
  }

  @NotNull
  private RouteProgress buildMockProgress(List<Point> remainingCoordinates) {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(remainingCoordinates);
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypoints()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    return routeProgress;
  }

  private List<Point> buildCoordinateList() {
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(Point.fromLngLat(1.234, 5.678));
    coordinates.add(Point.fromLngLat(9.012, 3.456));
    coordinates.add(Point.fromLngLat(7.890, 1.234));
    coordinates.add(Point.fromLngLat(5.678, 9.012));
    return coordinates;
  }
}
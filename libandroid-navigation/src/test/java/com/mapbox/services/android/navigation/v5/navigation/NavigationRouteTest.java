package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.EventListener;
import okhttp3.Interceptor;
import retrofit2.Call;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationRouteTest extends BaseTest {

  @Mock
  private Context context;
  @Mock
  private LocaleUtils localeUtils;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(localeUtils.inferDeviceLocale(context)).thenReturn(Locale.getDefault());
    when(localeUtils.getUnitTypeForDeviceLocale(context)).thenReturn(DirectionsCriteria.IMPERIAL);
  }

  @Test
  public void sanityTest() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .build();
    assertNotNull(navigationRoute);
  }

  @Test
  public void changingDefaultValueToCustomWorksProperly() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("/cycling/"));
  }

  @Test
  public void addApproachesIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addApproaches(DirectionsCriteria.APPROACH_CURB, DirectionsCriteria.APPROACH_UNRESTRICTED)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("curb"));
  }

  @Test
  public void checksWaypointIndicesIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .addWaypoint(Point.fromLngLat(1.0, 3.0))
      .addWaypoint(Point.fromLngLat(1.0, 3.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .addWaypointIndices(0, 2, 3)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("waypoints"));
  }

  @Test
  public void addWaypointNamesIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addWaypointNames("Origin", "Destination")
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("Destination"));
  }

  @Test
  public void addWaypointTargetsIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .addWaypointTargets(null, Point.fromLngLat(0.99, 4.99))
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("waypoint_targets"));
  }

  @Test
  public void addingPointAndBearingKeepsCorrectOrder() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0), 90d, 90d)
      .addBearing(2.0, 3.0)
      .destination(Point.fromLngLat(1.0, 5.0))
      .build();

    String requestUrl = navigationRoute.getCall().request().url().toString();
    assertThat(requestUrl, containsString("bearings=90%2C90%3B2%2C3%3B"));
  }

  @Test
  @Ignore
  public void reverseOriginDestinationDoesntMessUpBearings() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .destination(Point.fromLngLat(1.0, 5.0), 1d, 5d)
      .origin(Point.fromLngLat(1.0, 2.0), 90d, 90d)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("bearings=90,90;1,5"));
  }

  @Test
  public void addRouteOptionsIncludedInRequest() {
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(Point.fromLngLat(1.0, 2.0));
    coordinates.add(Point.fromLngLat(1.0, 3.0));
    coordinates.add(Point.fromLngLat(1.0, 5.0));

    RouteOptions routeOptions = RouteOptions.builder()
      .accessToken(ACCESS_TOKEN)
      .baseUrl("https://api-directions-traf.com")
      .requestUuid("XYZ_UUID")
      .alternatives(true)
      .language(Locale.US.getLanguage())
      .profile(DirectionsCriteria.PROFILE_WALKING)
      .coordinates(coordinates)
      .voiceUnits(DirectionsCriteria.METRIC)
      .user("example_user")
      .geometries("mocked_geometries")
      .approaches("curb;;unrestricted")
      .waypointNames("Origin;Pickup;Destination")
      .waypointTargets(";;0.99,4.99")
      .waypointIndices("0;2")
      .build();

    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .origin(coordinates.get(0))
      .addWaypoint(coordinates.get(1))
      .destination(coordinates.get(2))
      .routeOptions(routeOptions)
      .build();

    String request = navigationRoute.getCall().request().url().toString();
    assertThat(request, containsString("https://api-directions-traf.com"));
    assertThat(request, containsString("alternatives=true"));
    assertThat(request, containsString(ACCESS_TOKEN));
    assertThat(request, containsString("voice_units=metric"));
    assertThat(request, containsString("example_user"));
    assertThat(request, containsString("language=en"));
    assertThat(request, containsString("walking"));
    assertThat(request, containsString("curb"));
    assertThat(request, containsString("Origin"));
    assertThat(request, containsString("waypoint_targets"));
  }

  @Test
  public void cancelCall_cancelsCallNotExecuted() {
    MapboxDirections mapboxDirections = mock(MapboxDirections.class);
    Call<DirectionsResponse> routeCall = mock(Call.class);
    when(routeCall.isExecuted()).thenReturn(false);
    when(mapboxDirections.cloneCall()).thenReturn(routeCall);
    NavigationRoute navigationRoute = new NavigationRoute(mapboxDirections);

    navigationRoute.cancelCall();

    verify(routeCall).cancel();
  }

  @Test
  public void cancelCall_doesNotCancelExecutedCall() {
    MapboxDirections mapboxDirections = mock(MapboxDirections.class);
    Call<DirectionsResponse> routeCall = mock(Call.class);
    when(routeCall.isExecuted()).thenReturn(true);
    when(mapboxDirections.cloneCall()).thenReturn(routeCall);
    NavigationRoute navigationRoute = new NavigationRoute(mapboxDirections);

    navigationRoute.cancelCall();

    verify(routeCall, times(0)).cancel();
  }

  @Test
  public void builderInterceptor_setsMapboxDirections() {
    MapboxDirections.Builder mapboxDirectionsBuilder = mock(MapboxDirections.Builder.class);
    NavigationRoute.Builder builder = new NavigationRoute.Builder(mapboxDirectionsBuilder);
    EventListener eventListener = mock(EventListener.class);

    builder.eventListener(eventListener);

    verify(mapboxDirectionsBuilder).eventListener(eventListener);
  }

  @Test
  public void builderEventListener_setsMapboxDirections() {
    MapboxDirections.Builder mapboxDirectionsBuilder = mock(MapboxDirections.Builder.class);
    NavigationRoute.Builder builder = new NavigationRoute.Builder(mapboxDirectionsBuilder);
    Interceptor interceptor = mock(Interceptor.class);

    builder.interceptor(interceptor);

    verify(mapboxDirectionsBuilder).interceptor(interceptor);
  }

  @Test
  public void builderContinueStraight_setsMapboxDirections() {
    MapboxDirections.Builder mapboxDirectionsBuilder = mock(MapboxDirections.Builder.class);
    NavigationRoute.Builder builder = new NavigationRoute.Builder(mapboxDirectionsBuilder);
    boolean continueStraight = false;

    builder.continueStraight(continueStraight);

    verify(mapboxDirectionsBuilder).continueStraight(continueStraight);
  }
}

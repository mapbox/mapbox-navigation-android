package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.api.directions.v5.DirectionsCriteria;
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

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
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
  public void addBicycleTypeIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addBicycleType(DirectionsCriteria.ROAD)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("bicycle_type=Road"));
  }

  @Test
  public void addCyclingSpeedIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addCyclingSpeed(10.0f)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("cycling_speed=10.0"));
  }

  @Test
  public void addUseRoadsIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addUseRoads(0.5f)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("use_roads=0.5"));
  }

  @Test
  public void addUseHillsIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addUseHills(0.5f)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("use_hills=0.5"));
  }

  @Test
  public void addUseFerryIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addUseFerry(0.5f)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("use_ferry=0.5"));
  }

  @Test
  public void addAvoidBadSurfacesIncludedInRequest() {
    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .addAvoidBadSurfaces(0.5f)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("avoid_bad_surfaces=0.5"));
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
  public void addRouteOptionsIncludedInRequest() throws Exception {
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(Point.fromLngLat(1.0, 2.0));
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
      .approaches("curb;unrestricted")
      .waypointNames("Origin;Destination")
      .bicycleType(DirectionsCriteria.ROAD)
      .cyclingSpeed(10.0f)
      .useRoads(0.5f)
      .useHills(0.5f)
      .useFerry(0.5f)
      .avoidBadSurfaces(0.5f)
      .build();

    NavigationRoute navigationRoute = NavigationRoute.builder(context, localeUtils)
      .origin(coordinates.get(0))
      .destination(coordinates.get(1))
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
    assertThat(request, containsString("bicycle_type=Road"));
    assertThat(request, containsString("cycling_speed=10.0"));
    assertThat(request, containsString("use_roads=0.5"));
    assertThat(request, containsString("use_hills=0.5"));
    assertThat(request, containsString("use_ferry=0.5"));
    assertThat(request, containsString("avoid_bad_surfaces=0.5"));
  }
}

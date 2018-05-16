package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;

public class ValidationUtilsTest extends BaseTest {

  private static final String DIRECTIONS_WITHOUT_VOICE_INSTRUCTIONS = "directions_v5_no_voice.json";

  @Test(expected = MissingFormatArgumentException.class)
  public void validDirectionsRoute_isInvalidWithNullRouteOptions() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute(DIRECTIONS_WITHOUT_VOICE_INSTRUCTIONS);
    RouteOptions invalidRouteOptions = null;
    route = route.toBuilder().routeOptions(invalidRouteOptions).build();

    ValidationUtils.validDirectionsRoute(route, true);
  }

  @Test(expected = MissingFormatArgumentException.class)
  public void validDirectionsRoute_isInvalidWithNullInstructions() throws Exception {
    DirectionsRoute routeWithNullInstructions = buildRouteWithNullInstructions();

    ValidationUtils.validDirectionsRoute(routeWithNullInstructions, true);
  }

  @Test(expected = MissingFormatArgumentException.class)
  public void validDirectionsRoute_isInvalidWithFalseVoiceInstructions() throws Exception {
    DirectionsRoute routeWithFalseVoiceInstructions = buildRouteWithFalseVoiceInstructions();

    ValidationUtils.validDirectionsRoute(routeWithFalseVoiceInstructions, true);
  }

  @Test(expected = MissingFormatArgumentException.class)
  public void validDirectionsRoute_isInvalidWithFalseBannerInstructions() throws Exception {
    DirectionsRoute routeWithFalseBannerInstructions = buildRouteWithFalseBannerInstructions();

    ValidationUtils.validDirectionsRoute(routeWithFalseBannerInstructions, true);
  }

  private DirectionsRoute buildRouteWithNullInstructions() throws IOException {
    DirectionsRoute route = buildTestDirectionsRoute();
    List<Point> coordinates = new ArrayList<>();
    RouteOptions routeOptionsWithoutVoiceInstructions = RouteOptions.builder()
      .baseUrl(Constants.BASE_API_URL)
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .geometries("mocked_geometries")
      .coordinates(coordinates).build();

    return route.toBuilder()
      .routeOptions(routeOptionsWithoutVoiceInstructions)
      .build();
  }

  private DirectionsRoute buildRouteWithFalseVoiceInstructions() throws IOException {
    DirectionsRoute route = buildTestDirectionsRoute();
    List<Point> coordinates = new ArrayList<>();
    RouteOptions routeOptionsWithoutVoiceInstructions = RouteOptions.builder()
      .baseUrl(Constants.BASE_API_URL)
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .geometries("mocked_geometries")
      .voiceInstructions(false)
      .coordinates(coordinates).build();

    return route.toBuilder()
      .routeOptions(routeOptionsWithoutVoiceInstructions)
      .build();
  }

  private DirectionsRoute buildRouteWithFalseBannerInstructions() throws IOException {
    DirectionsRoute route = buildTestDirectionsRoute();
    List<Point> coordinates = new ArrayList<>();
    RouteOptions routeOptionsWithoutVoiceInstructions = RouteOptions.builder()
      .baseUrl(Constants.BASE_API_URL)
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .geometries("mocked_geometries")
      .voiceInstructions(true)
      .bannerInstructions(false)
      .coordinates(coordinates).build();

    return route.toBuilder()
      .routeOptions(routeOptionsWithoutVoiceInstructions)
      .build();
  }
}
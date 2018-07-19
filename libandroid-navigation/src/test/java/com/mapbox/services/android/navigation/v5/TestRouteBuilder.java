package com.mapbox.services.android.navigation.v5;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.mapbox.services.android.navigation.v5.BaseTest.ACCESS_TOKEN;
import static okhttp3.internal.Util.UTF_8;

class TestRouteBuilder {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  String loadJsonFixture(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  DirectionsRoute buildTestDirectionsRoute(@Nullable String fixtureName) throws IOException {
    fixtureName = checkNullFixtureName(fixtureName);
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(fixtureName);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.routes().get(0);
    return buildRouteWithOptions(route);
  }

  private DirectionsRoute buildRouteWithOptions(DirectionsRoute route) throws IOException {
    List<Point> coordinates = new ArrayList<>();
    RouteOptions routeOptionsWithoutVoiceInstructions = RouteOptions.builder()
      .baseUrl(Constants.BASE_API_URL)
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .geometries("mocked_geometries")
      .voiceInstructions(true)
      .bannerInstructions(true)
      .coordinates(coordinates).build();

    return route.toBuilder()
      .routeOptions(routeOptionsWithoutVoiceInstructions)
      .build();
  }

  @NonNull
  private String checkNullFixtureName(@Nullable String fixtureName) {
    if (fixtureName == null) {
      fixtureName = DIRECTIONS_PRECISION_6;
    }
    return fixtureName;
  }
}

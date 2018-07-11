package com.mapbox.services.android.navigation.v5.location.replay;

import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ReplayRouteParserTest {

  private static final double DELTA = 1e-15;

  @Test
  public void checksLongitudeParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(11.579233823791801, firstLocation.getLongitude(), DELTA);
  }

  @Test
  public void checksHorizontalAccuracyParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(40, firstLocation.getHorizontalAccuracyMeters(), DELTA);
  }

  @Test
  public void checksBearingParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(277.0355517432898, firstLocation.getBearing(), DELTA);
  }

  @Test
  public void checksVerticalAccuracyParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(10, firstLocation.getVerticalAccuracyMeters(), DELTA);
  }

  @Test
  public void checksSpeedParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(14.704089336389941, firstLocation.getSpeed(), DELTA);
  }

  @Test
  public void checksLatitudeParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(48.1776966801359, firstLocation.getLatitude(), DELTA);
  }

  @Test
  public void checksAltitudeParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    assertEquals(0, firstLocation.getAltitude(), DELTA);
  }

  @Test
  public void checksTimestampParsing() {
    String json = obtainJson("reroute.json");

    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);

    ReplayLocationDto firstLocation = routeFromJson.getLocations().get(0);
    String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    assertEquals("2018-06-25T18:16:11.005+0000", dateFormat.format(firstLocation.getDate()));
  }

  @Test
  public void checksRouteParsing() {
    String json = obtainJson("reroute.json");
    ReplayJsonRouteDto routeFromJson = new GsonBuilder().create().fromJson(json, ReplayJsonRouteDto.class);
    assertEquals("https://api.mapbox.com/directions/v5/mapbox/driving-traffic/11.579233823791801,48.1776966801359;" +
      "11.573521553454881,48.17812728496367.json?access_token=pk" +
      ".eyJ1IjoibWFwYm94LW5hdmlnYXRpb24iLCJhIjoiY2plZzkxZnl4MW9tZDMzb2R2ZXlkeHlhbCJ9.L1c9Wo-gk6d3cR3oi1n9SQ&steps" +
      "=true&overview=full&geometries=geojson", routeFromJson.getRouteRequest());
  }

  private String obtainJson(String fileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return convertStreamToString(classLoader.getResourceAsStream(fileName));
  }

  private String convertStreamToString(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}

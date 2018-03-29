package com.mapbox.services.android.navigation.ui.v5;

import com.google.gson.JsonParser;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;
import static okhttp3.internal.Util.UTF_8;

public class BaseTest {

  public static final double DELTA = 1E-10;
  public static final double LARGE_DELTA = 0.1;

  public static final String ACCESS_TOKEN = "pk.XXX";

  public void compareJson(String json1, String json2) {
    JsonParser parser = new JsonParser();
    assertEquals(parser.parse(json1), parser.parse(json2));
  }

  protected String loadJsonFixture(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  protected LegStep getFirstStep(DirectionsRoute route) {
    return route.legs().get(0).steps().get(0);
  }

  protected List<Point> buildStepPointsFromGeometry(String stepGeometry) {
    return PolylineUtils.decode(stepGeometry, PRECISION_6);
  }
}

package com.mapbox.services.android.navigation.ui.v5.map;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.emory.mathcs.backport.java.util.Collections;

import static okhttp3.internal.Util.UTF_8;
import static org.mockito.Mockito.mock;

public class WaynameFeatureFilterTest {

  @Test
  public void findPointFromCurrentPoint() {
    Feature featureOne = Feature.fromJson(loadJsonFixture("feature_one.json"));
    Point currentPoint = Point.fromLngLat(1.234, 4.567);
    WaynameFeatureFilter waynameFeatureFilter = buildFilter();

    Point featureAheadOfUser = waynameFeatureFilter.findPointFromCurrentPoint(currentPoint, (LineString) featureOne.geometry());
  }

  private List<Feature> buildQueriedFeatures() {
    List<Feature> queriedFeatures = new ArrayList<>();
    Feature featureOne = Feature.fromJson(loadJsonFixture("feature_one.json"));
    Feature featureTwo = Feature.fromJson(loadJsonFixture("feature_two.json"));
    queriedFeatures.add(featureOne);
    queriedFeatures.add(featureTwo);
    return queriedFeatures;
  }

  private String loadJsonFixture(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }

  @NonNull
  private WaynameFeatureFilter buildFilter() {
    return new WaynameFeatureFilter(Collections.emptyList(), mock(Location.class), Collections.emptyList());
  }
}
package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.RouteLeg;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class RouteAnnotationUpdaterTest {
  @Test
  public void updateRoute() {
    DirectionsRoute oldRoute =
      DirectionsRoute.builder().legs(getRouteLegs(getAnnotation(getOldCongestionAnnotations()))).build();
    DirectionsRoute newRoute =
      DirectionsRoute.builder().legs(getRouteLegs(getAnnotation(getNewCongestionAnnotations()))).build();

    DirectionsRoute updatedRoute = new RouteAnnotationUpdater().update(oldRoute, newRoute, 0);

    LegAnnotation expected = LegAnnotation.builder().congestion(getNewCongestionAnnotations()).build();
    assertEquals(expected, updatedRoute.legs().get(0).annotation());
  }

  private List<String> getOldCongestionAnnotations() {
    List<String> oldCongestionAnnotations = new ArrayList<>();
    oldCongestionAnnotations.add("zero");
    oldCongestionAnnotations.add("one");
    oldCongestionAnnotations.add("two");
    oldCongestionAnnotations.add("three");
    oldCongestionAnnotations.add("four");
    oldCongestionAnnotations.add("five");
    oldCongestionAnnotations.add("six");
    return oldCongestionAnnotations;
  }

  private List<String> getNewCongestionAnnotations() {
    List<String> newCongestionAnnotations = new ArrayList<>();
    newCongestionAnnotations.add("seven");
    newCongestionAnnotations.add("eight");
    newCongestionAnnotations.add("nine");
    newCongestionAnnotations.add("ten");
    newCongestionAnnotations.add("eleven");
    newCongestionAnnotations.add("twelve");
    newCongestionAnnotations.add("thirteen");
    return newCongestionAnnotations;
  }

  private List<RouteLeg> getRouteLegs(LegAnnotation legAnnotation) {
    return Collections.singletonList(RouteLeg.builder().annotation(legAnnotation).build());
  }

  private LegAnnotation getAnnotation(List<String> annotations) {
    return LegAnnotation.builder().congestion(annotations).build();
  }
}

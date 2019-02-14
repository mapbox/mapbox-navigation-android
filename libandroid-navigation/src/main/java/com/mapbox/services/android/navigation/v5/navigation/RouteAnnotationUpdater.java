package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.RouteLeg;

import java.util.ArrayList;
import java.util.List;

class RouteAnnotationUpdater {

  DirectionsRoute update(DirectionsRoute oldRoute, DirectionsRoute annotationHolder,
                         int currentLegIndex) {

    List<RouteLeg> legs = new ArrayList<>(oldRoute.legs());

    for (int i = currentLegIndex; i < legs.size(); i++) {
      LegAnnotation updatedAnnotation = annotationHolder.legs().get(i - currentLegIndex).annotation();
      legs.set(i, legs.get(i).toBuilder().annotation(updatedAnnotation).build());
    }

    return oldRoute.toBuilder()
      .legs(legs)
      .build();
  }
}

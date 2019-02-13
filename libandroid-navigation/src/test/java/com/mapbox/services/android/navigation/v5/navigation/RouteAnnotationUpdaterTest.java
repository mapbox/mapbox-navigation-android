package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.RouteLeg;

import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteAnnotationUpdaterTest {

  @Test
  public void updateRoute() {
    DirectionsRoute oldRoute = mock(DirectionsRoute.class);
    DirectionsRoute annotationHolder = mock(DirectionsRoute.class);
    RouteLeg originalRouteLeg = mock(RouteLeg.class);
    RouteLeg annotationRouteLeg = mock(RouteLeg.class);
    RouteLeg refreshedRouteLeg = mock(RouteLeg.class);
    RouteLeg.Builder originalRouteLegBuilder = mock(RouteLeg.Builder.class);
    when(originalRouteLegBuilder.annotation(any(LegAnnotation.class))).thenReturn(originalRouteLegBuilder);
    when(originalRouteLegBuilder.build()).thenReturn(refreshedRouteLeg);
    when(originalRouteLeg.toBuilder()).thenReturn(originalRouteLegBuilder);
    when(oldRoute.legs()).thenReturn(Collections.singletonList(originalRouteLeg));
    when(annotationHolder.legs()).thenReturn(Collections.singletonList(annotationRouteLeg));

    new RouteAnnotationUpdater().update(oldRoute, annotationHolder, 0);
  }

}

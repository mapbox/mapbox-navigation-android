package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.RouteLeg;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteAnnotationUpdaterTest {

  @Test
  public void updateRoute() {
    DirectionsRoute oldRoute = mock(DirectionsRoute.class);
    DirectionsRoute annotationHolder = mock(DirectionsRoute.class);
    DirectionsRoute.Builder routeBuilder = spy(DirectionsRoute.Builder.class);
    DirectionsRoute refreshedRoute = mock(DirectionsRoute.class);
    RouteLeg originalRouteLeg = mock(RouteLeg.class);
    RouteLeg annotationRouteLeg = mock(RouteLeg.class);
    List oldLegList = Collections.singletonList(originalRouteLeg);
    List newLegList = Collections.singletonList(annotationRouteLeg);
    RouteLeg refreshedRouteLeg = mock(RouteLeg.class);
    RouteLeg.Builder originalRouteLegBuilder = mock(RouteLeg.Builder.class);
    when(originalRouteLeg.toBuilder()).thenReturn(originalRouteLegBuilder);
    LegAnnotation legAnnotation = spy(LegAnnotation.class);
    when(originalRouteLegBuilder.annotation(legAnnotation)).thenReturn(originalRouteLegBuilder);
    when(originalRouteLegBuilder.build()).thenReturn(refreshedRouteLeg);

    when(oldRoute.legs()).thenReturn(oldLegList);
    when(oldRoute.toBuilder()).thenReturn(routeBuilder);
    when(routeBuilder.legs(any(List.class))).thenReturn(routeBuilder);
    when(routeBuilder.build()).thenReturn(refreshedRoute);
    when(annotationHolder.legs()).thenReturn(newLegList);

    DirectionsRoute resultRoute = new RouteAnnotationUpdater().update(oldRoute,
      annotationHolder, 0);

    verify(routeBuilder).legs(newLegList);
  }
}

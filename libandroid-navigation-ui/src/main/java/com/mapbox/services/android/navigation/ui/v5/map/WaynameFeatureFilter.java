package com.mapbox.services.android.navigation.ui.v5.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.mapbox.turf.TurfConstants.UNIT_METRES;
import static com.mapbox.turf.TurfMeasurement.along;
import static com.mapbox.turf.TurfMisc.lineSlice;
import static com.mapbox.turf.TurfMisc.lineSliceAlong;

class WaynameFeatureFilter {

  private static final int FIRST = 0;
  private static final int ONE_FEATURE = 1;
  private static final int TWO_POINTS = 2;
  private static final double ZERO_METERS = 0d;
  private static final double TEN = 10d;
  private final List<Feature> queriedFeatures;
  private final Point currentPoint;
  private final LineString currentStepLineString;

  WaynameFeatureFilter(List<Feature> queriedFeatures, Location currentLocation, List<Point> currentStepPoints) {
    this.queriedFeatures = new ArrayList<>(new HashSet<>(queriedFeatures));
    this.currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    this.currentStepLineString = LineString.fromLngLats(currentStepPoints);
  }

  @NonNull
  Feature filterFeatures() {
    return filterQueriedFeatures();
  }

  @NonNull
  private Feature filterQueriedFeatures() {
    Feature filteredFeature = queriedFeatures.get(FIRST);
    if (queriedFeatures.size() == ONE_FEATURE) {
      return filteredFeature;
    }
    double smallestUserDistanceToFeature = Double.POSITIVE_INFINITY;
    for (Feature feature : queriedFeatures) {
      Geometry featureGeometry = feature.geometry();
      if (featureGeometry == null) {
        continue;
      }
      List<LineString> featureLineStrings = new ArrayList<>();
      if (featureGeometry instanceof LineString) {
        featureLineStrings.add((LineString) featureGeometry);
      } else if (featureGeometry instanceof MultiLineString) {
        featureLineStrings = ((MultiLineString) featureGeometry).lineStrings();
      }

      for (LineString featureLineString : featureLineStrings) {
        List<Point> currentStepCoordinates = currentStepLineString.coordinates();
        int stepCoordinatesSize = currentStepCoordinates.size();
        if (stepCoordinatesSize < TWO_POINTS) {
          return filteredFeature;
        }
        int lastStepCoordinate = stepCoordinatesSize - 1;
        Point lastStepPoint = currentStepCoordinates.get(lastStepCoordinate);
        if (currentPoint.equals(lastStepPoint)) {
          return filteredFeature;
        }
        List<Point> lineCoordinates = featureLineString.coordinates();
        int lineCoordinatesSize = lineCoordinates.size();
        if (lineCoordinatesSize < TWO_POINTS) {
          return filteredFeature;
        }
        int lastLineCoordinate = lineCoordinatesSize - 1;
        Point lastLinePoint = lineCoordinates.get(lastLineCoordinate);
        if (currentPoint.equals(lastLinePoint)) {
          return filteredFeature;
        }

        Point firstLinePoint = lineCoordinates.get(FIRST);
        if (currentPoint.equals(firstLinePoint)) {
          return filteredFeature;
        }

        LineString stepSliceFromCurrentPoint = lineSlice(currentPoint, lastStepPoint, currentStepLineString);
        Point pointAheadUserOnStep = along(stepSliceFromCurrentPoint, TEN, UNIT_METRES);
        LineString reversedFeatureLine = reverseFeatureLineStringCoordinates(featureLineString);
        LineString currentAheadLine = reversedFeatureLine;
        LineString currentBehindLine = featureLineString;

        Point currentDirectionAhead = firstLinePoint;
        Point currentDirectionBehind = lastLinePoint;

        double distanceCurrentFirst = calculateDistance(currentPoint, firstLinePoint);
        double distanceAheadFirst = calculateDistance(pointAheadUserOnStep, firstLinePoint);
        if (distanceAheadFirst >= distanceCurrentFirst) {
          currentAheadLine = featureLineString;
          currentBehindLine = reversedFeatureLine;
          currentDirectionAhead = lastLinePoint;
          currentDirectionBehind = firstLinePoint;
        }

        LineString sliceFromCurrentPoint = lineSlice(currentPoint, currentDirectionAhead, currentAheadLine);
        Point pointAheadFeature = along(sliceFromCurrentPoint, TEN, UNIT_METRES);
        LineString reverseSliceFromCurrentPoint = lineSlice(currentPoint, currentDirectionBehind, currentBehindLine);
        Point pointBehindFeature = along(reverseSliceFromCurrentPoint, TEN, UNIT_METRES);

        double userDistanceToAheadFeature = calculateDistance(pointAheadUserOnStep, pointAheadFeature);
        double userDistanceToBehindFeature = calculateDistance(pointAheadUserOnStep, pointBehindFeature);
        double minDistanceToFeature = Math.min(userDistanceToAheadFeature, userDistanceToBehindFeature);

        if (minDistanceToFeature < smallestUserDistanceToFeature) {
          smallestUserDistanceToFeature = minDistanceToFeature;
          filteredFeature = feature;
        }
      }
    }
    return filteredFeature;
  }

  private double calculateDistance(Point lhs, Point rhs) {
    if (lhs == null || rhs == null) {
      return Double.POSITIVE_INFINITY;
    }
    return TurfMeasurement.distance(lhs, rhs);
  }

  @Nullable
  Point findPointFromCurrentPoint(Point currentPoint, LineString lineString) {
    List<Point> lineStringCoordinates = lineString.coordinates();
    int coordinateSize = lineStringCoordinates.size();
    if (coordinateSize < TWO_POINTS) {
      return null;
    }
    Point lastLinePoint = lineStringCoordinates.get(coordinateSize - 1);
    if (currentPoint == null || currentPoint.equals(lastLinePoint)) {
      return null;
    }
    LineString sliceFromCurrentPoint = lineSlice(currentPoint, lastLinePoint, lineString);
    LineString meterSlice = lineSliceAlong(sliceFromCurrentPoint, ZERO_METERS, (double) 10, UNIT_METRES);
    List<Point> slicePoints = meterSlice.coordinates();
    if (slicePoints.isEmpty()) {
      return null;
    }
    return slicePoints.get(FIRST);
  }

  @NonNull
  private LineString reverseFeatureLineStringCoordinates(LineString featureLineString) {
    List<Point> reversedFeatureCoordinates = new ArrayList<>(featureLineString.coordinates());
    Collections.reverse(reversedFeatureCoordinates);
    return LineString.fromLngLats(reversedFeatureCoordinates);
  }
}

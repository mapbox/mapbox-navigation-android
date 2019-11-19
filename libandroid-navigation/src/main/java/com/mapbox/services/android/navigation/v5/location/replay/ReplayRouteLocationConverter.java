package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;
import android.util.Log;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ReplayRouteLocationConverter {

    private static final int ONE_SECOND_IN_MILLISECONDS = 1000;
    private static final double ONE_KM_IN_METERS = 1000d;
    private static final int ONE_HOUR_IN_SECONDS = 3600;
    private static final double cosOf45 = 0.017452406437284;
    private static final int MAX_SUBDIVISIONS = 3;

    private static final String REPLAY_ROUTE = "com.mapbox.services.android.navigation.v5.location.replay"
            + ".ReplayRouteLocationEngine";
    private DirectionsRoute route;
    private int speed;
    private int delay;
    private double distance;
    private int currentLeg;
    private int currentStep;
    private long time;

    ReplayRouteLocationConverter(DirectionsRoute route, int speed, int delay) {
        initialize();
        update(route);
        this.speed = speed;
        this.delay = delay;
        this.distance = calculateDistancePerSec();
    }

    void updateSpeed(int customSpeedInKmPerHour) {
        this.speed = customSpeedInKmPerHour;
    }

    void updateDelay(int customDelayInSeconds) {
        this.delay = customDelayInSeconds;
    }

    List<Location> toLocations() {
        List<Point> stepPoints = calculateStepPoints();
        List<Location> mockedLocations = calculateMockLocations(stepPoints);

        return mockedLocations;
    }

    boolean isMultiLegRoute() {
        return route.legs().size() > 1;
    }

    void initializeTime() {
        time = System.currentTimeMillis();
    }

    /**
     * Interpolates the route into even points along the route and adds these to the points list.
     *
     * @param lineString our route geometry.
     * @return list of sliced {@link Point}s.
     */
    List<Point> sliceRoute(LineString lineString) {
        double distanceMeters = TurfMeasurement.length(lineString, TurfConstants.UNIT_METERS);
        if (distanceMeters <= 0) {
            return Collections.emptyList();
        }

        List<Point> points = new ArrayList<>();
        for (double i = 0; i < distanceMeters; i += distance) {
            Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);
            points.add(point);
        }
        return subdivideRouteSegment(points, MAX_SUBDIVISIONS);
    }

    List<Location> calculateMockLocations(List<Point> points) {
        List<Point> pointsToCopy = new ArrayList<>(points);
        List<Location> mockedLocations = new ArrayList<>();
        for (Point point : points) {
            Location mockedLocation = createMockLocationFrom(point);

            if (pointsToCopy.size() >= 2) {
                double bearing = TurfMeasurement.bearing(point, pointsToCopy.get(1));
                mockedLocation.setBearing((float) bearing);
            }
            time += delay * ONE_SECOND_IN_MILLISECONDS;
            mockedLocations.add(mockedLocation);
            pointsToCopy.remove(point);
        }

        return mockedLocations;
    }

    private List<Point> addSubdivisions(Point from, Point to, int count) {
        if (count == 0) {
            return null;
        }
        ArrayList<Point> pointList = new ArrayList<>();
        double deltaLng = (to.longitude() - from.longitude()) / count; // Compute midpoint if count is 2 or delta if count > 2
        double deltaLat = (to.latitude() - from.latitude()) / count;
        double fromLng = from.longitude();
        double fromLat = from.latitude();
        for (int i = 0; i < count; i++) {   // Generate higher resolution data
            fromLng += deltaLng;
            fromLat += deltaLat;
            pointList.add(Point.fromLngLat(fromLng, fromLat));
        }
        return pointList;
    }

    private double computeCosine(Point thisPoint, Point nextPoint){
        double u = thisPoint.latitude() * thisPoint.latitude() + thisPoint.longitude() * thisPoint.longitude();
        double v = nextPoint.latitude() * nextPoint.latitude() + nextPoint.longitude() * nextPoint.longitude();
        double numerator = thisPoint.latitude() * nextPoint.latitude() + thisPoint.longitude() * thisPoint.latitude();
        double denominator = u * v;
        double cosine = numerator / denominator;
        return cosine;
    }

    private ArrayList<Point>  computeSubdivisions(List<Point> points, int count, int index) {
        ArrayList<Point> retVal = new ArrayList<>();
        Point thisPoint = Point.fromLngLat(points.get(index).longitude(), points.get(index).latitude());
        Point nextPoint = Point.fromLngLat(points.get(index + 1).longitude(), points.get(index + 1).latitude());
        //Compute the cosine of the angle between two line segments
        double cosine = computeCosine(thisPoint, nextPoint);
        if (cosine <= cosOf45) {
            List<Point> subdivisions = addSubdivisions(thisPoint, nextPoint, count);
            if (subdivisions != null) {
                retVal.add(thisPoint);  // Automatically add the first point
                retVal.addAll(subdivisions);
                retVal.add(nextPoint);
            } else {
                retVal.add(thisPoint);
                retVal.add(nextPoint);
            }
        }
        return retVal;
    }

    private List<Point> subdivideRouteSegment(List<Point> points, int count) {
        ArrayList<Point> retVal = null;
        if (points.size() == 1){
            retVal = new ArrayList<>(points);
        } else if (points.size() == 2) {
            retVal = new ArrayList<>();
            retVal.addAll(computeSubdivisions(points, count, 0));
        } else {
            retVal = new ArrayList<>();
            for (int i = 0; i < points.size() - 2; i += 2) {
                retVal.addAll(computeSubdivisions(points, count, i));
            }
        }
        // If we got something back, return that. Otherwise, return the original list of points
        String debugMsg1 = "original points";
        String debugMsg2 = "computed points";
        if (retVal.isEmpty()) {
            return points;
        } else {
            StringBuilder stringBuilderDebug1 = new StringBuilder();
            StringBuilder stringBuilderDebug2 = new StringBuilder();
            stringBuilderDebug1.append(debugMsg1);
            stringBuilderDebug2.append(debugMsg2);
            stringBuilderDebug1.append(String.format("%s count: %d", debugMsg1, points.size()));
            stringBuilderDebug2.append(String.format("%s count: %d", debugMsg2, retVal.size()));

            for (Point p : points) {
                stringBuilderDebug1.append(String.format("[%.16f %.16f]", p.longitude(), p.latitude()));
            }
            for (Point pp : retVal) {
                stringBuilderDebug2.append(String.format("[%.16f %.16f]", pp.longitude(), pp.latitude()));
            }
            Log.d("DEBUG_TAG", stringBuilderDebug1.toString());
            Log.d("DEBUG_TAG", stringBuilderDebug2.toString());
            Log.d("DEBUG_TAG", "***************************************************");
        }
        return retVal;
    }


    private void update(DirectionsRoute route) {
        this.route = route;
    }

    /**
     * Converts the speed value to m/s and delay to seconds. Then the distance is calculated and returned.
     *
     * @return a double value representing the distance given a speed and time.
     */
    private double calculateDistancePerSec() {
        double distance = (speed * ONE_KM_IN_METERS * delay) / ONE_HOUR_IN_SECONDS;
        return distance;
    }

    private void initialize() {
        this.currentLeg = 0;
        this.currentStep = 0;
    }

    private List<Point> calculateStepPoints() {
        List<Point> stepPoints = new ArrayList<>();

        LineString line = LineString.fromPolyline(
                route.legs().get(currentLeg).steps().get(currentStep).geometry(), Constants.PRECISION_6);
        stepPoints.addAll(sliceRoute(line));
        increaseIndex();

        return stepPoints;
    }

    private void increaseIndex() {
        if (currentStep < route.legs().get(currentLeg).steps().size() - 1) {
            currentStep++;
        } else if (currentLeg < route.legs().size() - 1) {
            currentLeg++;
            currentStep = 0;
        }
    }

    private Location createMockLocationFrom(Point point) {
        Location mockedLocation = new Location(REPLAY_ROUTE);
        mockedLocation.setLatitude(point.latitude());
        mockedLocation.setLongitude(point.longitude());
        float speedInMetersPerSec = (float) ((speed * ONE_KM_IN_METERS) / ONE_HOUR_IN_SECONDS);
        mockedLocation.setSpeed(speedInMetersPerSec);
        mockedLocation.setAccuracy(15f);
        mockedLocation.setTime(time);
        return mockedLocation;
    }
}

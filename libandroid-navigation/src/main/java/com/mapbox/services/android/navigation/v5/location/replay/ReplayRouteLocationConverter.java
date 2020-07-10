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
    private static final String REPLAY_ROUTE = "ReplayRouteLocation";
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
        if (lineString == null || lineString.coordinates().isEmpty()) {
            return Collections.emptyList();
        }

        double distanceMeters = TurfMeasurement.length(lineString, TurfConstants.UNIT_METERS);
        if (distanceMeters <= 0) {
            return Collections.emptyList();
        }

        List<Point> points = new ArrayList<>();
        for (double i = 0; i < distanceMeters; i += distance) {
            Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);
            points.add(point);
        }
        return points;
    }

    List<Location> calculateMockLocations(List<Point> points) {
        List<Location> mockedLocations = new ArrayList<>();
        for(int i = 0; i < points.size(); i++){
            Location mockedLocation = createMockLocationFrom(points.get(i));

            if (i - 1 >= 0) {
                double bearing = TurfMeasurement.bearing(points.get(i - 1), points.get(i));
                mockedLocation.setBearing((float) bearing);
            }else{
                mockedLocation.setBearing(0);
            }
            time += delay * ONE_SECOND_IN_MILLISECONDS;
            mockedLocations.add(mockedLocation);
        }

        return mockedLocations;
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
        mockedLocation.setAccuracy(3f);
        mockedLocation.setTime(time);
        return mockedLocation;
    }
}

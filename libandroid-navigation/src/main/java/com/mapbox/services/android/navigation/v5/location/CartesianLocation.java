package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

import timber.log.Timber;

public class CartesianLocation {

  private static final double EARTH_RADIUS = 6367;
  private double xCoordinate;
  private double yCoordinate;
  private double zCoordinate;

  public CartesianLocation(Location location) {

    Timber.d("Ingest Location: %s", location);
    xCoordinate = calculateX(location.getLatitude(), location.getLongitude());
    yCoordinate = calculateY(location.getLatitude(), location.getLongitude());
    zCoordinate = calculateZ(location.getLatitude());
  }

  public double getxCoordinate() {
    return xCoordinate;
  }

  public double getyCoordinate() {
    return yCoordinate;
  }

  public double getzCoordinate() {
    return zCoordinate;
  }

  public void setzCoordinate(double newZ) {
    this.zCoordinate = newZ;
  }

  public void setxCoordinate(double xCoordinate) {
    this.xCoordinate = xCoordinate;
  }

  public void setyCoordinate(double yCoordinate) {
    this.yCoordinate = yCoordinate;
  }

  public Location getGeodeticLocation() {
    double r = Math.sqrt(xCoordinate * xCoordinate + yCoordinate * yCoordinate + zCoordinate * zCoordinate);
    double lat = Math.toDegrees(Math.asin(zCoordinate / r));
    double lon = Math.toDegrees(Math.atan2(yCoordinate, xCoordinate));

    Location geodeticLocation = new Location("cartesian");
    geodeticLocation.setLatitude(lat);
    geodeticLocation.setLongitude(lon);

    return geodeticLocation;
  }

  double calculateX(double lat, double lon) {
    return EARTH_RADIUS * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lon));
  }

  double calculateY(double lat, double lon) {
    return EARTH_RADIUS * Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(lon));
  }

  double calculateZ(double lat) {
    return EARTH_RADIUS * Math.sin(Math.toRadians(lat));
  }
}
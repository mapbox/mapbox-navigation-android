package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class NavigationLocationData implements Parcelable {
  private Location[] locationsBefore;
  private Location[] locationsAfter;

  public NavigationLocationData(Location[] locationsBefore, Location[] locationsAfter) {
    this.locationsBefore = locationsBefore;
    this.locationsAfter = locationsAfter;
  }

  Location[] getLocationsBefore() {
    return locationsBefore;
  }

  Location[] getLocationsAfter() {
    return locationsAfter;
  }

  private NavigationLocationData(Parcel in) {
    locationsBefore = in.createTypedArray(Location.CREATOR);
    locationsAfter = in.createTypedArray(Location.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedArray(locationsBefore, flags);
    dest.writeTypedArray(locationsAfter, flags);
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationLocationData> CREATOR = new Creator<NavigationLocationData>() {
    @Override
    public NavigationLocationData createFromParcel(Parcel in) {
      return new NavigationLocationData(in);
    }

    @Override
    public NavigationLocationData[] newArray(int size) {
      return new NavigationLocationData[size];
    }
  };
}

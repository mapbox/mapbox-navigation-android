package com.mapbox.services.android.navigation.ui.v5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.HashMap;

public class NavigationLauncher {

  public static void startNavigation(Activity activity, DirectionsRoute route) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, new Gson().toJson(route));
    editor.apply();

    Intent navigationView = new Intent(activity, NavigationView.class);
    Bundle bundle = new Bundle();
    bundle.putBoolean(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, true);
    navigationView.putExtras(bundle);
    activity.startActivity(navigationView);
  }

  public static void startNavigation(Activity activity, Position origin, Position destination) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LAT_KEY,
      Double.doubleToRawLongBits(origin.getLatitude()));
    editor.putLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LNG_KEY,
      Double.doubleToRawLongBits(origin.getLongitude()));
    editor.putLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LAT_KEY,
      Double.doubleToRawLongBits(destination.getLatitude()));
    editor.putLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LNG_KEY,
      Double.doubleToRawLongBits(destination.getLongitude()));
    editor.apply();

    Intent navigationView = new Intent(activity, NavigationView.class);
    Bundle bundle = new Bundle();
    bundle.putBoolean(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, false);
    navigationView.putExtras(bundle);
    activity.startActivity(navigationView);
  }

  static DirectionsRoute extractRoute(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String directionsRoute = preferences.getString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, "");
    return new Gson().fromJson(directionsRoute, DirectionsRoute.class);
  }

  static HashMap<String, Position> extractCoordinates(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    double originLng = Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LNG_KEY, 0));
    double originLat =  Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LAT_KEY, 0));
    double destinationLng =  Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LNG_KEY, 0));
    double destinationLat =  Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LAT_KEY, 0));

    Position origin = Position.fromLngLat(originLng, originLat);
    Position destination = Position.fromLngLat(destinationLng, destinationLat);

    HashMap<String, Position> coordinates = new HashMap<>();
    coordinates.put(NavigationConstants.NAVIGATION_VIEW_ORIGIN, origin);
    coordinates.put(NavigationConstants.NAVIGATION_VIEW_DESTINATION, destination);
    return coordinates;
  }
}

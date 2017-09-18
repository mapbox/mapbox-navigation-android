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

/**
 * Use this class to launch the navigation UI
 * <p>
 * You can launch the UI with either a route you have already retrieved from
 * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationRoute} or you can pass a
 * {@link Position} origin and {@link Position} destination and the UI will request the {@link DirectionsRoute}
 * while initializing.
 * </p><p>
 * You have an option to include a AWS Cognito Pool ID, which will initialize the UI with AWS Polly Voice instructions
 * </p><p>
 * For testing, you can launch with simulation, in which our
 * {@link com.mapbox.services.android.location.MockLocationEngine} will begin
 * following the given {@link DirectionsRoute} once the UI is initialized
 * </p>
 */
public class NavigationLauncher {

  /**
   * Starts the UI with a {@link DirectionsRoute} already retrieved from
   * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationRoute}
   *
   * @param activity      must be launched from another {@link Activity}
   * @param route         initial route in which the navigation will follow
   * @param awsPoolId     used to activate AWS Polly (if null, will use to {@link android.speech.tts.TextToSpeech})
   * @param simulateRoute if true, will mock location movement - if false, will use true location
   */
  public static void startNavigation(Activity activity, DirectionsRoute route,
                                     String awsPoolId, boolean simulateRoute) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, new Gson().toJson(route));

    editor.putString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, awsPoolId);
    editor.putBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, simulateRoute);
    editor.apply();

    Intent navigationView = new Intent(activity, NavigationView.class);
    Bundle bundle = new Bundle();
    bundle.putBoolean(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, true);
    navigationView.putExtras(bundle);
    activity.startActivity(navigationView);
  }

  /**
   * Starts the UI with a {@link Position} origin and {@link Position} destination which will allow the UI
   * to retrieve a {@link DirectionsRoute} upon initialization
   *
   * @param activity      must be launched from another {@link Activity}
   * @param origin        where you want to start navigation (most likely your current location)
   * @param destination   where you want to navigate to
   * @param awsPoolId     used to activate AWS Polly (if null, will use to {@link android.speech.tts.TextToSpeech})
   * @param simulateRoute if true, will mock location movement - if false, will use true location
   */
  public static void startNavigation(Activity activity, Position origin, Position destination,
                                     String awsPoolId, boolean simulateRoute) {
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

    editor.putString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, awsPoolId);
    editor.putBoolean(NavigationConstants.NAVIGATION_VIEW_SIMULATE_ROUTE, simulateRoute);
    editor.apply();

    Intent navigationView = new Intent(activity, NavigationView.class);
    Bundle bundle = new Bundle();
    bundle.putBoolean(NavigationConstants.NAVIGATION_VIEW_LAUNCH_ROUTE, false);
    navigationView.putExtras(bundle);
    activity.startActivity(navigationView);
  }

  /**
   * Used to extract the route used to launch the drop-in UI.
   * <p>
   * Extracts the route {@link String} from {@link SharedPreferences} and converts
   * it back to a {@link DirectionsRoute} object with {@link Gson}.
   *
   * @param context to retrieve {@link SharedPreferences}
   * @return {@link DirectionsRoute} stored when launching
   */
  static DirectionsRoute extractRoute(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String directionsRoute = preferences.getString(NavigationConstants.NAVIGATION_VIEW_ROUTE_KEY, "");
    return new Gson().fromJson(directionsRoute, DirectionsRoute.class);
  }

  /**
   * Used to extract the origin and position coordinates used to launch
   * the drop-in UI.
   * <p>
   * A {@link HashMap} is used to ensure the correct coordinate is
   * extracted in the {@link NavigationView} with the defined constants.
   *
   * @param context to retrieve {@link SharedPreferences}
   * @return map with both origin and destination coordinates
   */
  static HashMap<String, Position> extractCoordinates(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    double originLng = Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LNG_KEY, 0));
    double originLat = Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LAT_KEY, 0));
    double destinationLng = Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LNG_KEY, 0));
    double destinationLat = Double.longBitsToDouble(preferences
      .getLong(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LAT_KEY, 0));

    Position origin = Position.fromLngLat(originLng, originLat);
    Position destination = Position.fromLngLat(destinationLng, destinationLat);

    HashMap<String, Position> coordinates = new HashMap<>();
    coordinates.put(NavigationConstants.NAVIGATION_VIEW_ORIGIN, origin);
    coordinates.put(NavigationConstants.NAVIGATION_VIEW_DESTINATION, destination);
    return coordinates;
  }
}

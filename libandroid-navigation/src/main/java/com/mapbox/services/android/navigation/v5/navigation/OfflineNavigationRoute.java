package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.core.utils.TextUtils;

import java.util.List;

import okhttp3.HttpUrl;

public class OfflineNavigationRoute {

  private static final String BICYCLE_TYPE_QUERY_PARAMETER = "bicycle_type";
  private static final String CYCLING_SPEED_QUERY_PARAMETER = "cycling_speed";
  private static final String USE_ROADS_QUERY_PARAMETER = "use_roads";
  private static final String USE_HILLS_QUERY_PARAMETER = "use_hills";
  private static final String USE_FERRY_QUERY_PARAMETER = "use_ferry";
  private static final String AVOID_BAD_SURFACES_QUERY_PARAMETER = "avoid_bad_surfaces";
  private static final String WAYPOINT_TYPES_QUERY_PARAMETER = "waypoint_types";


  /**
   * Builds a URL string for offline.
   *
   * @param route a {@link NavigationRoute}
   * @param offlineOptions the {@link OfflineNavigationOptions}
   * @return the offline url string
   */
  public static String buildUrl(@NonNull NavigationRoute route, @NonNull OfflineNavigationOptions offlineOptions) {
    checkNonNullRoute(route);
    checkNonNullOptions(offlineOptions);
    String onlineUrl = route.getCall().request().url().toString();
    String offlineUrl = buildOfflineUrl(onlineUrl, offlineOptions);
    return offlineUrl;
  }

  private static void checkNonNullRoute(NavigationRoute route) {
    if (route == null) {
      throw new IllegalArgumentException("NavigationRoute must be non-null");
    }
  }

  private static void checkNonNullOptions(OfflineNavigationOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("OfflineNavigationOptions must be non-null");
    }
  }

  private static String buildOfflineUrl(String onlineUrl, OfflineNavigationOptions offlineOptions) {
    if (offlineOptions == null) {
      return onlineUrl;
    }
    HttpUrl.Builder offlineUrlBuilder = HttpUrl.get(onlineUrl).newBuilder();
    if (isBicycleType(offlineOptions.bicycleType())) {
      offlineUrlBuilder.addQueryParameter(BICYCLE_TYPE_QUERY_PARAMETER, offlineOptions.bicycleType());
    }

    if (offlineOptions.cyclingSpeed() != null) {
      offlineUrlBuilder.addQueryParameter(CYCLING_SPEED_QUERY_PARAMETER, offlineOptions.cyclingSpeed().toString());
    }

    if (offlineOptions.useRoads() != null) {
      offlineUrlBuilder.addQueryParameter(USE_ROADS_QUERY_PARAMETER, offlineOptions.useRoads().toString());
    }

    if (offlineOptions.useHills() != null) {
      offlineUrlBuilder.addQueryParameter(USE_HILLS_QUERY_PARAMETER, offlineOptions.useHills().toString());
    }

    if (offlineOptions.useFerry() != null) {
      offlineUrlBuilder.addQueryParameter(USE_FERRY_QUERY_PARAMETER, offlineOptions.useFerry().toString());
    }

    if (offlineOptions.avoidBadSurfaces() != null) {
      offlineUrlBuilder
        .addQueryParameter(AVOID_BAD_SURFACES_QUERY_PARAMETER, offlineOptions.avoidBadSurfaces().toString());
    }

    if (offlineOptions.waypointTypes() != null) {
      String formattedWaypointTypes = formatWaypointTypes(offlineOptions.waypointTypes());
      if (formattedWaypointTypes == null) {
        throw new ServicesException("All waypoint types values must be one of break, through or null");
      }
      offlineUrlBuilder.addQueryParameter(WAYPOINT_TYPES_QUERY_PARAMETER, formattedWaypointTypes);
    }
    return offlineUrlBuilder.build().toString();
  }

  private static boolean isBicycleType(String type) {
    if (TextUtils.isEmpty(type)) {
      return false;
    }
    if (!type.equals(OfflineCriteria.ROAD) && !type.equals(OfflineCriteria.HYBRID)
      && !type.equals(OfflineCriteria.CITY) && !type.equals(OfflineCriteria.CROSS)
      && !type.equals(OfflineCriteria.MOUNTAIN)) {
      throw new ServicesException("Bicycle type value must be one of Road, Hybrid, City, Cross or Mountain");
    }
    return true;
  }

  private static String formatWaypointTypes(List<String> waypointTypesToFormat) {
    String[] waypointTypes = waypointTypesToFormat.toArray(new String[0]);
    for (int i = 0; i < waypointTypes.length; i++) {
      if (waypointTypes[i] == null) {
        waypointTypes[i] = "";
      } else if (!waypointTypes[i].equals(OfflineCriteria.BREAK)
        && !waypointTypes[i].equals(OfflineCriteria.THROUGH) && !waypointTypes[i].isEmpty()) {
        return null;
      }
    }
    return TextUtils.join(";", waypointTypes);
  }
}

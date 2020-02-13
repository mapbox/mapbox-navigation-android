package com.mapbox.services.android.navigation.testapp.activity;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryActivity extends AppCompatActivity {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
  private static final String JSON_EXTENSION = ".json";

  private MapboxNavigation navigation;
  private String filename;
  private RouteProgressObserver progressHistoryListener = (routeProgress) -> executeStoreHistoryTask();
  private LocationObserver enhancedLocationObserver = new LocationObserver() {

    @Override
    public void onEnhancedLocationChanged(@NotNull Location enhancedLocation) {
      executeStoreHistoryTask();
    }

    @Override
    public void onRawLocationChanged(@NotNull Location rawLocation) {
    }
  };

  public void addNavigationForHistory(@NonNull MapboxNavigation navigation) {
    if (navigation == null) {
      throw new IllegalArgumentException("MapboxNavigation cannot be null");
    }
    this.navigation = navigation;
    navigation.registerRouteProgressObserver(progressHistoryListener);
    navigation.registerLocationObserver(enhancedLocationObserver);
    navigation.toggleHistory(true);
    filename = buildFileName();
  }

  protected void executeStoreHistoryTask() {
    new StoreHistoryTask(navigation, filename).execute();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigation.toggleHistory(false);
  }

  private String buildFileName() {
    StringBuilder sb = new StringBuilder();
    sb.append(obtainCurrentTimeStamp());
    sb.append(JSON_EXTENSION);
    return sb.toString();
  }

  private String obtainCurrentTimeStamp() {
    Date now = new Date();
    String strDate = DATE_FORMAT.format(now);
    return strDate;
  }
}

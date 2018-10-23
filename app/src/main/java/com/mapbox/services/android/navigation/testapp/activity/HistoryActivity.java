package com.mapbox.services.android.navigation.testapp.activity;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryActivity extends AppCompatActivity {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
  private static final String JSON_EXTENSION = ".json";

  private MapboxNavigation navigation;
  private String filename;

  public void addNavigationForHistory(@NonNull MapboxNavigation navigation) {
    if (navigation == null) {
      throw new IllegalArgumentException("MapboxNavigation cannot be null");
    }
    this.navigation = navigation;
    navigation.addProgressChangeListener(progressHistoryListener);
    navigation.toggleHistory(true);
    filename = buildFileName();
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

  private ProgressChangeListener progressHistoryListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      new StoreHistoryTask(navigation, filename).execute();
    }
  };
}

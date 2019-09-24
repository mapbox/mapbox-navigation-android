package com.mapbox.services.android.navigation.testapp.activity;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryActivity extends AppCompatActivity {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
  private static final String JSON_EXTENSION = ".json";

  private MapboxNavigation navigation;
  private String filename;
  private ProgressChangeListener progressHistoryListener = (location, routeProgress) -> executeStoreHistoryTask();

  public void addNavigationForHistory(@NonNull MapboxNavigation navigation) {
    if (navigation == null) {
      throw new IllegalArgumentException("MapboxNavigation cannot be null");
    }
    this.navigation = navigation;
    navigation.addProgressChangeListener(progressHistoryListener);
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

package com.mapbox.services.android.navigation.testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mapbox.services.android.navigation.testapp.activity.NavigationActivity;
import com.mapbox.services.android.navigation.testapp.activity.OffRouteDetectionActivity;
import com.mapbox.services.android.navigation.testapp.activity.RouteUtilsV5Activity;
import com.mapbox.services.android.navigation.testapp.activity.SnapToRouteActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final List<SampleItem> samples = new ArrayList<>(Arrays.asList(
      new SampleItem(
        getString(R.string.title_route_utils_v5),
        getString(R.string.description_route_utils),
        RouteUtilsV5Activity.class
      ),
      new SampleItem(
        getString(R.string.title_navigation),
        getString(R.string.description_navigation),
        NavigationActivity.class
      ),
      new SampleItem(
        getString(R.string.title_snap_to_route),
        getString(R.string.description_snap_to_route),
        SnapToRouteActivity.class
      ),
      new SampleItem(
        getString(R.string.title_off_route_detection),
        getString(R.string.description_off_route_detection),
        OffRouteDetectionActivity.class
      )
    ));
  }
}

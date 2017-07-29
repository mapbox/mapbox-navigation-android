package com.mapbox.services.android.navigation.testapp.activity;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ServiceTestActivity extends AppCompatActivity {

  @BindView(R.id.toggleServiceFab)
  FloatingActionButton floatingActionButton;

  private MapboxNavigation mapboxNavigation;
  private boolean isRunning;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_service_test);
    ButterKnife.bind(this);

    mapboxNavigation = new MapboxNavigation(this);
    mapboxNavigation.addMilestoneEventListener(new MilestoneEventListener() {
      @Override
      public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
        System.out.println(instruction);
      }
    });

  }

  @OnClick(R.id.toggleServiceFab)
  public void onToggleServiceFabClick(View view) {
    isRunning = !isRunning;
    Snackbar.make(view, "Service running? " + isRunning, Snackbar.LENGTH_LONG).show();
//    if (isRunning) {
//      mapboxNavigation.startNavigation();
//    } else {
//      mapboxNavigation.endNavigation();
//    }
  }
}

package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

public class NavigationViewActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_view);

    Button launchNavigationBtn = (Button) findViewById(R.id.launchBtn);
    launchNavigationBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        launchNavigation();
      }
    });
  }

  private void launchNavigation() {
    Intent navigationView = new Intent(this, NavigationView.class);
    Bundle coordinates = new Bundle();
    // Washington, D.C.
    coordinates.putDouble(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LNG_KEY, -77.009003);
    coordinates.putDouble(NavigationConstants.NAVIGATION_VIEW_ORIGIN_LAT_KEY, 38.889931);

    // Arlington, VA
    coordinates.putDouble(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LNG_KEY, -77.100703);
    coordinates.putDouble(NavigationConstants.NAVIGATION_VIEW_DESTINATION_LAT_KEY, 38.878337);

    navigationView.putExtras(coordinates);
    startActivity(navigationView);
  }
}

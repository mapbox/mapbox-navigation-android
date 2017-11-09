package com.mapbox.services.android.navigation.ui.v5;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

public class NavigationActivity extends AppCompatActivity implements NavigationViewListener {

  private NavigationView navigationView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    navigationView = findViewById(R.id.navigationView);
    String customMapStyleUrl;
    if (getIntent().getStringExtra(NavigationConstants.NAVIGATION_VIEW_CUSTOM_MAP_STYLE_URL) != null) {
      customMapStyleUrl = getIntent().getStringExtra(NavigationConstants.NAVIGATION_VIEW_CUSTOM_MAP_STYLE_URL);
      navigationView.onCreate(savedInstanceState, customMapStyleUrl);
    } else {
      navigationView.onCreate(savedInstanceState, null);
    }
    navigationView.getNavigationAsync(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  protected void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
  }

  @Override
  public void onBackPressed() {
    // If the navigation view didn't need to do anything, call super
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onNavigationReady() {
    navigationView.startNavigation(this);
  }

  @Override
  public void onNavigationFinished() {
    finish();
  }
}

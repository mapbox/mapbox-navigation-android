package com.mapbox.services.android.navigation.testapp;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.widget.TextView;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Serves as a launching point for the custom drop-in UI, {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
 * <p>
 * Demonstrates the proper setup and usage of the view, including all lifecycle methods.
 */
public class EmbeddedNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener, ProgressChangeListener {

  private NavigationView navigationView;
  private View spacer;
  private TextView speedWidget;
  private Point origin = Point.fromLngLat(-77.03194990754128, 38.909664963450105);
  private Point destination = Point.fromLngLat(-77.0270025730133, 38.91057077063121);

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_embedded_navigation);
    navigationView = findViewById(R.id.navigationView);
    navigationView.onCreate(savedInstanceState);

    speedWidget = findViewById(R.id.speed_limit);
    spacer = findViewById(R.id.spacer);
    setSpeedWidgetAnchor(R.id.summaryBottomSheet);

    navigationView.getNavigationAsync(this);
  }

  private void setSpeedWidgetAnchor(@IdRes int res) {
    CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacer.getLayoutParams();
    layoutParams.setAnchorId(res);
    spacer.setLayoutParams(layoutParams);
  }

  @Override
  public void onNavigationReady() {
    NavigationViewOptions.Builder options = NavigationViewOptions.builder();
    options.navigationListener(this);
    options.origin(origin);
    options.destination(destination);
    options.shouldSimulateRoute(true);
    options.progressChangeListener(this);
    setBottomSheetCallback(options);

    navigationView.startNavigation(options.build());
  }

  private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
    options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View bottomSheet, int newState) {
        switch (newState) {
          case BottomSheetBehavior.STATE_HIDDEN:
            setSpeedWidgetAnchor(R.id.recenterBtn);
            break;
          case BottomSheetBehavior.STATE_EXPANDED:
          case BottomSheetBehavior.STATE_COLLAPSED:
            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
        }
      }

      @Override
      public void onSlide(@NonNull View bottomSheet, float slideOffset) {

      }
    });
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onBackPressed() {
    // If the navigation view didn't need to do anything, call super
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
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
  public void onCancelNavigation() {
    // Navigation canceled, finish the activity
    finish();
  }

  @Override
  public void onNavigationFinished() {
    // Intentionally empty
  }

  @Override
  public void onNavigationRunning() {
    // Intentionally empty
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    setSpeed(location);
  }

  private void setSpeed(Location location) {
    String string1 = Integer.toString((int)(location.getSpeed() * 2.2369));
    String string2 = "\nMPH";
    SpannableString spannableString1 = new SpannableString(string1);
    spannableString1.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.speed_text_size)), 0, string1.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    SpannableString spannableString2 = new SpannableString(string2);
    spannableString2.setSpan(new AbsoluteSizeSpan(getResources().getDimensionPixelSize(R.dimen.mph_text_size)), 0, string2.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);


    speedWidget.setText(TextUtils.concat(spannableString1, spannableString2));
    speedWidget.setVisibility(View.VISIBLE);
  }
}

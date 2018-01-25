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

public class EmbeddedNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
  NavigationListener, ProgressChangeListener {

  private NavigationView navigationView;
  private View spacer;
  private TextView speedWidget;
  private Point origin = Point.fromLngLat(-77.03194990754128, 38.909664963450105);
  private Point destination = Point.fromLngLat(-77.0270025730133, 38.91057077063121);
  private boolean bottomSheetVisible = true;

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

  /**
   * Sets the anchor of the spacer for the speed widget, thus setting the anchor for the speed widget
   * (The speed widget is anchored to the spacer, which is there because padding between items and
   * their anchors in CoordinatorLayouts is finicky.
   * @param res resource for view of which to anchor the spacer
   */
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
            bottomSheetVisible = false;
            setSpeedWidgetAnchor(R.id.recenterBtn);
            break;
          case BottomSheetBehavior.STATE_EXPANDED:
            bottomSheetVisible = true;
            break;
          case BottomSheetBehavior.STATE_SETTLING:
            if (!bottomSheetVisible) {
              // View needs to be anchored to the bottom sheet before it is finished expanding
              // because of the animation
              setSpeedWidgetAnchor(R.id.summaryBottomSheet);
            }
            break;
          default:
            return;
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
    String string = String.format("%d\nMPH", (int) (location.getSpeed() * 2.2369));
    int mphTextSize = getResources().getDimensionPixelSize(R.dimen.mph_text_size);
    int speedTextSize = getResources().getDimensionPixelSize(R.dimen.speed_text_size);

    SpannableString spannableString = new SpannableString(string);
    spannableString.setSpan(new AbsoluteSizeSpan(mphTextSize),
      string.length() - 4, string.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

    spannableString.setSpan(new AbsoluteSizeSpan(speedTextSize),
      0, string.length() - 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

    speedWidget.setText(spannableString);
    speedWidget.setVisibility(View.VISIBLE);
  }
}

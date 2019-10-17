package com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.services.android.navigation.testapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FragmentNavigationActivity extends AppCompatActivity {

  private static final String FAB_VISIBLE_KEY = "restart_fab_visible";

  @BindView(R.id.restart_navigation_fab)
  FloatingActionButton restartNavigationFab;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_fragment);
    ButterKnife.bind(this);
    initializeNavigationViewFragment(savedInstanceState);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(FAB_VISIBLE_KEY, restartNavigationFab.getVisibility() == View.VISIBLE);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    boolean isVisible = savedInstanceState.getBoolean(FAB_VISIBLE_KEY);
    int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
    restartNavigationFab.setVisibility(visibility);
  }

  @OnClick(R.id.restart_navigation_fab)
  public void onClick(FloatingActionButton restartNavigationFab) {
    replaceFragment(new NavigationFragment());
    restartNavigationFab.hide();
  }

  public void showNavigationFab() {
    restartNavigationFab.show();
  }

  public void showPlaceholderFragment() {
    replaceFragment(new PlaceholderFragment());
  }

  private void initializeNavigationViewFragment(@Nullable Bundle savedInstanceState) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    if (savedInstanceState == null) {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.disallowAddToBackStack();
      transaction.add(R.id.navigation_fragment_frame, new NavigationFragment()).commit();
    }
  }

  private void replaceFragment(Fragment newFragment) {
    String tag = String.valueOf(newFragment.getId());
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.disallowAddToBackStack();
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
    int fadeInAnimId = androidx.appcompat.R.anim.abc_fade_in;
    int fadeOutAnimId = androidx.appcompat.R.anim.abc_fade_out;
    transaction.setCustomAnimations(fadeInAnimId, fadeOutAnimId, fadeInAnimId, fadeOutAnimId);
    transaction.replace(R.id.navigation_fragment_frame, newFragment, tag);
    transaction.commit();
  }
}

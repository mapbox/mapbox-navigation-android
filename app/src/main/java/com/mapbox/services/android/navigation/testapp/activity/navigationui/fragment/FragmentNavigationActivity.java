package com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.services.android.navigation.testapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FragmentNavigationActivity extends AppCompatActivity {

  @BindView(R.id.restart_navigation_fab)
  FloatingActionButton restartNavigationFab;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_fragment);
    ButterKnife.bind(this);
    initializeNavigationViewFragment(savedInstanceState);
  }

  @OnClick(R.id.restart_navigation_fab)
  public void onClick(FloatingActionButton restartNavigationFab) {
    replaceFragment(new NavigationFragment(), true);
    restartNavigationFab.hide();
  }

  public void showNavigationFab() {
    restartNavigationFab.show();
  }

  public void showPlaceholderFragment() {
    replaceFragment(new PlaceholderFragment(), false);
  }

  private void initializeNavigationViewFragment(@Nullable Bundle savedInstanceState) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    if (savedInstanceState == null) {
      FragmentTransaction ft = fragmentManager.beginTransaction();
      ft.add(R.id.navigation_fragment_frame, new NavigationFragment()).commit();
    }
  }

  private void replaceFragment(Fragment newFragment, boolean addToBackStack) {
    String tag = String.valueOf(newFragment.getId());
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
    int fadeInAnimId = android.support.v7.appcompat.R.anim.abc_fade_in;
    int fadeOutAnimId = android.support.v7.appcompat.R.anim.abc_fade_out;
    transaction.setCustomAnimations(fadeInAnimId, fadeOutAnimId, fadeInAnimId, fadeOutAnimId);
    transaction.replace(R.id.navigation_fragment_frame, newFragment, tag);
    if (addToBackStack) {
      transaction.addToBackStack(tag);
    }
    transaction.commit();
  }
}

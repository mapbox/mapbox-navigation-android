package com.mapbox.services.android.navigation.testapp.activity.navigationui;


import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.mapbox.services.android.navigation.testapp.R;

public class NavigationViewSettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentManager().beginTransaction().replace(
      android.R.id.content, new NavigationViewPreferenceFragment()).commit();
  }

  public static class NavigationViewPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.fragment_navigation_view_preferences);
      PreferenceManager.setDefaultValues(getActivity(), R.xml.fragment_navigation_view_preferences, false);
    }
  }
}

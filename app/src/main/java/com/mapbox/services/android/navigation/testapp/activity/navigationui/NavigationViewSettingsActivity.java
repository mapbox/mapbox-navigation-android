package com.mapbox.services.android.navigation.testapp.activity.navigationui;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.mapbox.services.android.navigation.testapp.R;

public class NavigationViewSettingsActivity extends PreferenceActivity {
  private SharedPreferences.OnSharedPreferenceChangeListener listener;
  static final String UNIT_TYPE_CHANGED = "unit_type_changed";
  static final String LANGUAGE_CHANGED = "language_changed";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    listener = (sharedPreferences, key) -> {
      Intent resultIntent = new Intent();
      resultIntent.putExtra(UNIT_TYPE_CHANGED, key.equals(getString(R.string.unit_type_key)));
      resultIntent.putExtra(LANGUAGE_CHANGED, key.equals(getString(R.string.language_key)));
      setResult(RESULT_OK, resultIntent);
    };
    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    getFragmentManager().beginTransaction().replace(
      android.R.id.content, new NavigationViewPreferenceFragment()).commit();
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return super.isValidFragment(fragmentName);
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

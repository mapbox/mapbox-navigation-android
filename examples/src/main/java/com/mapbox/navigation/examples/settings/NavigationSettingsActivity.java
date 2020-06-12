package com.mapbox.navigation.examples.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.mapbox.navigation.examples.BuildConfig;
import com.mapbox.navigation.examples.R;

public class NavigationSettingsActivity extends PreferenceActivity {

  public static final String UNIT_TYPE_CHANGED = "unit_type_changed";
  public static final String LANGUAGE_CHANGED = "language_changed";
  public static final String OFFLINE_CHANGED = "offline_changed";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
      Intent resultIntent = new Intent();
      resultIntent.putExtra(UNIT_TYPE_CHANGED, key.equals(getString(R.string.unit_type_key)));
      resultIntent.putExtra(LANGUAGE_CHANGED, key.equals(getString(R.string.language_key)));
      resultIntent.putExtra(OFFLINE_CHANGED, key.equals(getString(R.string.offline_version_key)));
      setResult(RESULT_OK, resultIntent);
    };

    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    getFragmentManager().beginTransaction().replace(
        android.R.id.content, new NavigationViewPreferenceFragment()
    ).commit();
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return super.isValidFragment(fragmentName);
  }

  public static class NavigationViewPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.fragment_navigation_preferences);

      String gitHashTitle = String.format("Last Commit Hash: %s", BuildConfig.GIT_HASH);
      findPreference(getString(R.string.git_hash_key)).setTitle(gitHashTitle);
    }

    @Override
    public void onResume() {
      super.onResume();

      PreferenceManager.setDefaultValues(getActivity(), R.xml.fragment_navigation_preferences, false);
    }
  }
}

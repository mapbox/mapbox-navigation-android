package com.mapbox.services.android.navigation.testapp;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class NavigationSettingsActivity extends PreferenceActivity {

  public static final String UNIT_TYPE_CHANGED = "unit_type_changed";
  public static final String LANGUAGE_CHANGED = "language_changed";
  public static final String OFFLINE_CHANGED = "offline_changed";
  private static final int EXTERNAL_STORAGE_PERMISSION = 1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
      Intent resultIntent = new Intent();
      resultIntent.putExtra(UNIT_TYPE_CHANGED, key.equals(getString(R.string.unit_type_key)));
      resultIntent.putExtra(LANGUAGE_CHANGED, key.equals(getString(R.string.language_key)));
      resultIntent.putExtra(OFFLINE_CHANGED, key.equals(getString(R.string.offline_preference_key)));
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
      checkOfflinePermission();
    }

    @Override
    public void onResume() {
      super.onResume();

      getOfflineVersions();
      PreferenceManager.setDefaultValues(getActivity(), R.xml.fragment_navigation_preferences, false);
    }

    private void getOfflineVersions() {
      File file = new File(Environment.getExternalStoragePublicDirectory("Offline"), "tiles");
      if (!file.exists()) {
        file.mkdirs();
      }

      List<String> list;

      if (file.list() != null && file.list().length != 0) {
        list = new ArrayList<>(Arrays.asList(file.list()));
      } else {
        list = new ArrayList<>();
      }
      list.add(getString(R.string.offline_disabled));

      ListPreference offlineVersions =
        (ListPreference) findPreference(getString(R.string.offline_preference_key));
      offlineVersions.setOnPreferenceClickListener(preference -> checkOfflinePermission());

      String[] entries = list.toArray(new String[list.size()]);
      offlineVersions.setEntries(entries);
      offlineVersions.setEntryValues(entries);
    }

    private boolean checkOfflinePermission() {
      if (ContextCompat.checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
          getActivity(), new String[] { WRITE_EXTERNAL_STORAGE }, EXTERNAL_STORAGE_PERMISSION);
        return false;
      }
      return true;
    }
  }
}

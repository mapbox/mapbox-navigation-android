package com.mapbox.navigation.examples.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.mapbox.navigation.examples.BuildConfig;
import com.mapbox.navigation.examples.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NavigationSettingsActivity extends FragmentActivity {

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
    getSupportFragmentManager().beginTransaction().replace(
      android.R.id.content, new NavigationViewPreferenceFragment()
    ).commit();
  }

  public static class NavigationViewPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      addPreferencesFromResource(R.xml.fragment_navigation_preferences);

      String gitHashTitle = String.format("Last Commit Hash: %s", BuildConfig.GIT_HASH);
      findPreference(getString(R.string.git_hash_key)).setTitle(gitHashTitle);
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

      ListPreference offlineVersions = (ListPreference) findPreference(getString(R.string.offline_version_key));
      List<String> list = buildFileList(file);
      if (!list.isEmpty()) {
        String[] entries = list.toArray(new String[list.size() - 1]);
        offlineVersions.setEntries(entries);
        offlineVersions.setEntryValues(entries);
        offlineVersions.setEnabled(true);
      } else {
        offlineVersions.setEnabled(false);
      }
    }

    @NonNull
    private List<String> buildFileList(File file) {
      List<String> list;
      if (file.list() != null && file.list().length != 0) {
        list = new ArrayList<>(Arrays.asList(file.list()));
      } else {
        list = new ArrayList<>();
      }
      return list;
    }
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.content.SharedPreferences;

class PreferenceManager {

  private SharedPreferences preferences;
  private final Object lock = new Object();

  PreferenceManager(Context context, String prefKey) {
    this.preferences = context.getSharedPreferences(prefKey, Context.MODE_PRIVATE);
  }

  public <T> void set(String key, T value) {
    synchronized (lock) {
      SharedPreferences.Editor editor = preferences.edit();
      if (value instanceof Boolean) {
        editor.putBoolean(key, (Boolean) value);
      } else if (value instanceof String) {
        editor.putString(key, (String) value);
      } else if (value instanceof Float) {
        editor.putFloat(key, (Float) value);
      } else if (value instanceof Long) {
        editor.putLong(key, (Long) value);
      } else if (value instanceof Integer) {
        editor.putInt(key, (Integer) value);
      }
      editor.apply();
    }
  }

  public <T> T get(String key, T defaultValue) {
    synchronized (lock) {
      if (defaultValue instanceof Boolean) {
        Boolean result = preferences.getBoolean(key, (Boolean) defaultValue);
        return (T) result;
      } else if (defaultValue instanceof String) {
        String result = preferences.getString(key, (String) defaultValue);
        return (T) result;
      } else if (defaultValue instanceof Float) {
        Float result = preferences.getFloat(key, (Float) defaultValue);
        return (T) result;
      } else if (defaultValue instanceof Long) {
        Long result = preferences.getLong(key, (Long) defaultValue);
        return (T) result;
      } else if (defaultValue instanceof Integer) {
        Integer result = preferences.getInt(key, (Integer) defaultValue);
        return (T) result;
      }
      return null;
    }
  }
}

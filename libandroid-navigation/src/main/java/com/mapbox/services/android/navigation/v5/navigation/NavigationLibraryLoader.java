package com.mapbox.services.android.navigation.v5.navigation;

import timber.log.Timber;

public abstract class NavigationLibraryLoader {

  private static final String NAVIGATION_NATIVE = "navigator-android";
  private static final NavigationLibraryLoader DEFAULT = new NavigationLibraryLoader() {
    @Override
    public void load(String name) {
      System.loadLibrary(name);
    }
  };

  private static volatile NavigationLibraryLoader loader = DEFAULT;

  /**
   * Loads navigation shared library.
   * <p>
   * Catches UnsatisfiedLinkErrors and prints a warning to logcat.
   * </p>
   */
  public static void load() {
    try {
      loader.load(NAVIGATION_NATIVE);
    } catch (UnsatisfiedLinkError error) {
      Timber.e(error, "Failed to load native shared library.");
    }
  }

  public abstract void load(String name);
}

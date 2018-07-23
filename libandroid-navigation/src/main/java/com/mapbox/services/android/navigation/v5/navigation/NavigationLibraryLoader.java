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
   * Set the library loader that loads the shared library.
   *
   * @param libraryLoader the library loader
   */
  public static void setLibraryLoader(NavigationLibraryLoader libraryLoader) {
    loader = libraryLoader;
  }

  /**
   * Loads navigation native shared library.
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

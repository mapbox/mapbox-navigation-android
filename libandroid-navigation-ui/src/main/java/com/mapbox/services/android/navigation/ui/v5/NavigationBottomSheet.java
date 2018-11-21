package com.mapbox.services.android.navigation.ui.v5;

/**
 * Used to display summary information in the {@link NavigationView}.
 */
public interface NavigationBottomSheet extends ReplaceableNavigationComponent {

  /**
   * When invoked, hides the bottom sheet view (INVISIBLE).
   */
  void hide();

  /**
   * When invoked, shows the bottom sheet view (VISIBLE).
   */
  void show();
}

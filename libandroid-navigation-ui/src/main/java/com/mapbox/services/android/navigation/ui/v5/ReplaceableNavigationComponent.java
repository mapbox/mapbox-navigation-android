package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.LayoutRes;
import android.view.View;

/**
 * This interface defines the methods that allow you to
 * replace the {@link NavigationView} UI components that implement it.
 */
public interface ReplaceableNavigationComponent {

  /**
   * Replace this component with a pre-built {@link View}.
   *
   * @param view to be used in place of the component.
   */
  void replaceWith(View view);

  /**
   * Replace this component with a layout resource ID.  The component
   * will inflate and add the layout once it is ready.
   *
   * @param layoutResId to be inflated and added
   * @param listener    to notify when the replacement is finished
   */
  void replaceWith(@LayoutRes int layoutResId, OnLayoutReplacedListener listener);
}

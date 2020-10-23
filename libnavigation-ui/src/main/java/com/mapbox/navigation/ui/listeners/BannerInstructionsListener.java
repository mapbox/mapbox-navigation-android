package com.mapbox.navigation.ui.listeners;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;

/**
 * This listener will be triggered when a {@link BannerInstructions} is about to be displayed.
 * <p>
 * The listener gives you the option to override any values and pass as the return value,
 * which will be the value used for the banner instructions.
 */
public interface BannerInstructionsListener {

  /**
   * Listener tied to {@link BannerInstructions} that are about to be displayed.
   * <p>
   * To prevent the given {@link BannerInstructions} from being displayed, you can return null
   * and it will be ignored.
   *
   * @param instructions about to be displayed
   * @return instructions to be displayed; null if should be ignored
   */
  @Nullable
  BannerInstructions willDisplay(BannerInstructions instructions);
}

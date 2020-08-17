package com.mapbox.navigation.ui.instruction;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.BannerComponents;

/**
 * This is the default text coordinator implementation to handle plain text components.
 */
class TextCreator extends NodeCreator<BannerComponentNode, TextVerifier> {
  TextCreator() {
    this(new TextVerifier());
  }

  private TextCreator(TextVerifier textVerifier) {
    super(textVerifier);
  }

  @NonNull
  @Override
  BannerComponentNode setupNode(BannerComponents components, int index, int startIndex, String
    modifier) {
    return new BannerComponentNode(components, startIndex);
  }
}

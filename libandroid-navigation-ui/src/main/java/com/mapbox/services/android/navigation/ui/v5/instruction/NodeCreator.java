package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import java.util.List;

abstract class NodeCreator<N extends BannerComponentNode, V extends NodeVerifier> {
  V nodeVerifier;

  NodeCreator(V nodeVerifier) {
    this.nodeVerifier = nodeVerifier;
  }

  boolean isNodeType(BannerComponents bannerComponents) {
    return nodeVerifier.isNodeType(bannerComponents);
  }

  abstract N setupNode(BannerComponents components, int index, int startIndex, String modifier);

  /**
   * One coordinator should override this method, and this should be the coordinator which populates
   * the textView with text.
   *
   * @param textView to populate
   * @param bannerComponentNodes containing instructions
   */
  void preProcess(TextView textView, List<BannerComponentNode> bannerComponentNodes) {}

  /**
   * Coordinators which make edits to the text after it's been populated into the text view should
   * override this method. This includes coordinators which load images into the text view.
   *
   * @param textView to populate
   * @param bannerComponentNodes containing instructions
   */
  void postProcess(TextView textView, List<BannerComponentNode> bannerComponentNodes) {}
}

package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerText;

import org.junit.Test;
import org.mockito.InOrder;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class BannerComponentTreeTest {

  @Test
  public void parseComponents() {
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents();
    List<BannerComponents> bannerComponentsList = Collections.singletonList(bannerComponents);
    TestCreator testCreator = mock(TestCreator.class);
    when(testCreator.isNodeType(bannerComponents)).thenReturn(true);
    BannerText bannerText = mock(BannerText.class);
    when(bannerText.components()).thenReturn(bannerComponentsList);

    new BannerComponentTree(bannerText, testCreator);

    verify(testCreator).setupNode(bannerComponents, 0, 0, null);
  }

  @Test
  public void loadInstruction() {
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponents();
    List<BannerComponents> bannerComponentsList = Collections.singletonList(bannerComponents);
    TestNode testNode = mock(TestNode.class);
    TestCreator testCreator = mock(TestCreator.class);
    when(testCreator.isNodeType(bannerComponents)).thenReturn(true);
    when(testCreator.setupNode(bannerComponents, 0, 0, null)).thenReturn(testNode);
    TextView textView = mock(TextView.class);
    BannerText bannerText = mock(BannerText.class);
    when(bannerText.components()).thenReturn(bannerComponentsList);
    BannerComponentTree bannerComponentTree = new BannerComponentTree(bannerText, testCreator);

    bannerComponentTree.loadInstruction(textView);

    InOrder inOrder = inOrder(testCreator, testCreator);
    inOrder.verify(testCreator).preProcess(any(TextView.class), any(List.class));
    inOrder.verify(testCreator).postProcess(any(TextView.class), any(List.class));
  }

  class TestNode extends BannerComponentNode {
    TestNode(BannerComponents bannerComponents, int startIndex) {
      super(bannerComponents, startIndex);
    }
  }

  class TestVerifier implements NodeVerifier {

    @Override
    public boolean isNodeType(BannerComponents bannerComponents) {
      return true;
    }
  }

  class TestCreator extends NodeCreator<TestNode, TestVerifier> {
    TestCreator(TestVerifier nodeVerifier) {
      super(nodeVerifier);
    }

    @Override
    TestNode setupNode(BannerComponents components, int index, int startIndex, String modifier) {
      return new TestNode(components, startIndex);
    }
  }
}

package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.widget.TextView;

import com.mapbox.api.directions.v5.models.BannerComponents;

import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BannerComponentTreeTest {

  @Test
  public void loadInstruction() {
    List<BannerComponents> bannerComponentsList = new ArrayList<>();
    BannerComponents bannerComponents = BannerComponentsFaker.bannerComponentsWithAbbreviation();
    bannerComponentsList.add(bannerComponents);
    TestNode testNode = mock(TestNode.class);
    TestCreator nodeCoordinator = mock(TestCreator.class);
    when(nodeCoordinator.isNodeType(bannerComponents)).thenReturn(true);
    when(nodeCoordinator.setupNode(any(BannerComponents.class), anyInt(), anyInt())).thenReturn(testNode);
    TextView textView = mock(TextView.class);
    BannerComponentTree bannerComponentTree = new BannerComponentTree(bannerComponentsList, nodeCoordinator);
    bannerComponentTree.loadInstruction(textView);

    InOrder inOrder = inOrder(nodeCoordinator, nodeCoordinator);
    inOrder.verify(nodeCoordinator).preProcess(any(TextView.class), any(List.class));
    inOrder.verify(nodeCoordinator).postProcess(any(TextView.class), any(List.class));
  }

  class TestNode extends BannerComponentNode {
    TestNode(BannerComponents bannerComponents, int startIndex) {
      super(bannerComponents, startIndex);
    }
  }

  class TestVerifier extends NodeVerifier {

    @Override
    boolean isNodeType(BannerComponents bannerComponents) {
      return true;
    }
  }


  class TestCreator extends NodeCreator<TestNode, TestVerifier> {
    TestCreator(TestVerifier nodeVerifier) {
      super(nodeVerifier);
    }

    @Override
    TestNode setupNode(BannerComponents components, int index, int startIndex) {
      return new TestNode(components, startIndex);
    }
  }

}

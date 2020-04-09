package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.OfflineMetadataProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OfflineMetadataProviderTest {

  @Test
  public void checksOfflineRouteMetadataCreation() {
    String aRouteSummary = "cjuykbm4705v26pnpvqlbjm5n";
    OfflineMetadataProvider theOfflineMetadataProvider = new OfflineMetadataProvider();

    byte[] routeSummaryMetadata = theOfflineMetadataProvider.buildMetadataFor(aRouteSummary);

    assertEquals("{\"route_summary\":\"cjuykbm4705v26pnpvqlbjm5n\"}", new String(routeSummaryMetadata));
  }
}
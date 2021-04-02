package com.mapbox.navigation.ui.maps;
//
//import androidx.annotation.NonNull;
//
//import com.mapbox.bindgen.Expected;
//import com.mapbox.bindgen.ExpectedFactory;
//import com.mapbox.common.TileStore;
//import com.mapbox.maps.ResourceOptions;
//import com.mapbox.maps.TileStoreManager;
//
//import org.robolectric.annotation.Implementation;
//import org.robolectric.annotation.Implements;
//
///**
// * To avoid calling native method of ValueConverter, this shadow ValueConverter
// * will be used for the Robolectric unit tests.
// */
//@Implements(TileStoreManager.class)
//public class ShadowTileStoreManager {
//
//  @Implementation
//  public static @NonNull
//  Expected<TileStore, String> getTileStore(ResourceOptions resourceOptions) {
//    return ExpectedFactory.createValue();
//  }
//}

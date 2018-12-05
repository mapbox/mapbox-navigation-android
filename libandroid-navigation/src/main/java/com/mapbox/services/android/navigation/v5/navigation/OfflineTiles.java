package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.routetiles.v1.MapboxRouteTiles;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.BoundingBox;

import retrofit2.Callback;

/**
 * This is a wrapper class for the {@link MapboxRouteTiles} class. This class takes care of
 * interfacing with {@link MapboxRouteTiles} and receives a TAR file wrapped in a ResponseBody
 * which can be handled using a
 * {@link com.mapbox.services.android.navigation.v5.utils.DownloadTask}.
 */
public class OfflineTiles {

  private final MapboxRouteTiles mapboxRouteTiles;
  private final String version;

  private OfflineTiles(MapboxRouteTiles mapboxRouteTiles, String version) {
    this.mapboxRouteTiles = mapboxRouteTiles;
    this.version = version;
  }

  /**
   * Gets a new Builder to build an {@link OfflineTiles} object
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Call when you have constructed your OfflineTiles object with your desired parameters.
   * {@link Callback} must be passed into the method to handle both the response and failure.
   *
   * @param callback for retrofit
   */
  void fetchRouteTiles(Callback callback) {
    mapboxRouteTiles.enqueueCall(callback);
  }

  /**
   * Returns the version of the current builder.
   *
   * @return the version of the current builder
   */
  public String version() {
    return version;
  }

  /**
   * This builder is used to create a new request to the Mapbox Route Tiles API. A request and
   * therefore a builder must include a version, access token, and a {@link BoundingBox}.
   */
  public static class Builder {
    private final MapboxRouteTiles.Builder mapboxRouteTilesBuilder;
    private String version;

    Builder() {
      mapboxRouteTilesBuilder = MapboxRouteTiles.builder();
    }

    /**
     * The string version for the tile set being requested. To fetch all available versions, use
     * {@link OfflineTileVersions}.
     *
     * @param version of tiles being requested
     * @return this builder for chaining options together
     */
    public Builder version(String version) {
      this.version = version;
      mapboxRouteTilesBuilder.version(version);
      return this;
    }

    /**
     *
     * Required to call when this is being built. If no access token provided,
     * {@link ServicesException} will be thrown by the {@link MapboxRouteTiles.Builder}.
     *
     * @param accessToken Mapbox access token, You must have a Mapbox account inorder to use the
     *                    Optimization API
     * @return this builder for chaining options together
     */
    public Builder accessToken(String accessToken) {
      mapboxRouteTilesBuilder.accessToken(accessToken);
      return this;
    }

    /**
     * The bounding box representing the region of tiles being requested. The API can handle a
     * maximum of 1.5 million square kilometers.
     *
     * @param boundingBox representing the region
     * @return this builder for chaining options together
     */
    public Builder boundingBox(BoundingBox boundingBox) {
      mapboxRouteTilesBuilder.boundingBox(boundingBox);
      return this;
    }

    /**
     * Builds a new OfflineTiles object.
     *
     * @return a new instance of OfflineTiles
     */
    public OfflineTiles build() {
      return new OfflineTiles(mapboxRouteTilesBuilder.build(), version);
    }
  }
}

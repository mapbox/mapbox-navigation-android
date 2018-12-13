package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Response;

class TarResponseErrorMap {

  private static final int TILES_ACCESS_TOKEN_ERROR_CODE = 402;
  private static final int BOUNDING_BOX_ERROR_CODE = 422;
  private static final String TILES_ACCESS_TOKEN_ERROR_MESSAGE = "Unable to fetch tiles: Before you can fetch "
    + "routing tiles you must obtain an enterprise access token. Please contact us at support@mapbox.com";
  private static final String BOUNDING_BOX_ERROR_MESSAGE = "Unable to fetch tiles: The bounding box you have "
    + "specified is too large. Please select a smaller box and try again.";
  private static final String ERROR_MESSAGE_FORMAT = "Error code %s: %s";
  private final HashMap<Integer, String> errorCodes;

  TarResponseErrorMap(@NonNull HashMap<Integer, String> errorCodes) {
    this.errorCodes = errorCodes;
    errorCodes.put(TILES_ACCESS_TOKEN_ERROR_CODE, TILES_ACCESS_TOKEN_ERROR_MESSAGE);
    errorCodes.put(BOUNDING_BOX_ERROR_CODE, BOUNDING_BOX_ERROR_MESSAGE);
  }

  @NonNull
  String buildErrorMessageWith(@NonNull Response<ResponseBody> response) {
    String errorMessage = errorCodes.get(response.code());
    if (errorMessage == null) {
      errorMessage = String.format(ERROR_MESSAGE_FORMAT, response.code(), response.message());
      return errorMessage;
    }
    return errorMessage;
  }
}

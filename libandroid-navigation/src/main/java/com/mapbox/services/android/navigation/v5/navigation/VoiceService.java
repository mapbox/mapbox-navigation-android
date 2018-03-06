package com.mapbox.services.android.navigation.v5.navigation;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface VoiceService {
  @GET("/voice/v1/speak/{text}")
  Call<ResponseBody> getInstruction(
    @Path("text") String text,
    @Query("textType") String textType,
    @Query("language") String language,
    @Query("outputFormat") String outputFormat,
    @Query("access_token") String accessToken);
}

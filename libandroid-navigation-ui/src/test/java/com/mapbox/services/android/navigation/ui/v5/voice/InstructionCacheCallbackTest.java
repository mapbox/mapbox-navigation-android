package com.mapbox.services.android.navigation.ui.v5.voice;

import org.junit.Test;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InstructionCacheCallbackTest {

  @Test
  public void onResponse_cachedUrlIsAdded() {
    VoiceInstructionLoader loader = mock(VoiceInstructionLoader.class);
    Response<ResponseBody> response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(response.body()).thenReturn(body);
    String url = "http://some.url";
    Call call = buildMockCall(url);
    InstructionCacheCallback callback = new InstructionCacheCallback(loader);

    callback.onResponse(call, response);

    verify(loader).addCachedUrl(eq(url));
  }

  @Test
  public void onResponse_bodyIsClosed() {
    VoiceInstructionLoader loader = mock(VoiceInstructionLoader.class);
    Response<ResponseBody> response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(response.body()).thenReturn(body);
    String url = "http://some.url";
    Call call = buildMockCall(url);
    InstructionCacheCallback callback = new InstructionCacheCallback(loader);

    callback.onResponse(call, response);

    verify(body).close();
  }

  @Test
  public void onResponse_nullBodyIsIgnored() {
    VoiceInstructionLoader loader = mock(VoiceInstructionLoader.class);
    Response<ResponseBody> response = mock(Response.class);
    String url = "http://some.url";
    Call call = buildMockCall(url);
    InstructionCacheCallback callback = new InstructionCacheCallback(loader);

    callback.onResponse(call, response);

    verifyZeroInteractions(loader);
  }

  private Call buildMockCall(String stringUrl) {
    Call call = mock(Call.class);
    Request request = mock(Request.class);
    HttpUrl url = mock(HttpUrl.class);
    when(url.toString()).thenReturn(stringUrl);
    when(request.url()).thenReturn(url);
    when(call.request()).thenReturn(request);
    return call;
  }
}
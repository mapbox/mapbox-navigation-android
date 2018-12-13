package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TarResponseErrorMapTest {

  @Test
  public void buildErrorMessage_402messageIsCreated() {
    HashMap<Integer, String> errorCodes = new HashMap<>();
    Response<ResponseBody> response = mock(Response.class);
    when(response.code()).thenReturn(402);
    TarResponseErrorMap errorMap = new TarResponseErrorMap(errorCodes);

    String errorMessage = errorMap.buildErrorMessageWith(response);

    assertTrue(errorMessage.contains("Please contact us at support@mapbox.com"));
  }

  @Test
  public void buildErrorMessage_messageIsCreatedForCodeNotFound() {
    HashMap<Integer, String> errorCodes = new HashMap<>();
    Response<ResponseBody> response = mock(Response.class);
    when(response.code()).thenReturn(100);
    when(response.message()).thenReturn("Some error message");
    TarResponseErrorMap errorMap = new TarResponseErrorMap(errorCodes);

    String errorMessage = errorMap.buildErrorMessageWith(response);

    assertEquals("Error code 100: Some error message", errorMessage);
  }
}
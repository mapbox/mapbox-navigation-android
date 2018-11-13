package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;

import com.mapbox.services.android.navigation.ui.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoiceInstructionLoaderTest extends BaseTest {

  @Test
  public void checksEvictFourInstructions() throws Exception {
    Context anyContext = mock(Context.class);
    Cache aCache = mock(Cache.class);
    List<String> urlsToBeCached = buildUrlsToCache();
    urlsToBeCached.remove(4);
    Iterator<String> urlsCached = urlsToBeCached.iterator();
    when(aCache.urls()).thenReturn(urlsCached);
    VoiceInstructionLoader theVoiceInstructionLoader = new VoiceInstructionLoader(anyContext, "any_access_token",
      aCache);
    List<String> urlsToCache = buildUrlsToCache();
    theVoiceInstructionLoader.addStubUrlsToCache(urlsToCache);

    List<String> urlsEvicted = theVoiceInstructionLoader.evictVoiceInstructions();

    assertEquals(4, urlsEvicted.size());
  }

  private List<String> buildUrlsToCache() {
    List<String> urlsCached = new ArrayList<>();
    urlsCached.add("https://api.mapbox.com/voice/v1/speak/%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody" +
      "%20rate=%221.08%22%3EIn%20200%20meters," +
      "%20enter%20the%20traffic%20circle%20and%20take%20the%203rd%20exit%20onto%20%3Csay-as%20interpret-as=%22address" +
      "%22%3E14th%3C%2Fsay-as%3E%20Street%20Northwest%3C%2Fprosody%3E%3C%2Famazon:effect%3E%3C%2Fspeak%3E?textType" +
      "=ssml&language=en&access_token=pk.XXX");
    urlsCached.add("https://api.mapbox.com/voice/v1/speak/%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody" +
      "%20rate=%221.08%22%3EContinue%20on%20%3Csay-as%20interpret-as=%22address%22%3E14th%3C%2Fsay-as%3E%20Street" +
      "%20Northwest%20for%20600%20meters%3C%2Fprosody%3E%3C%2Famazon:effect%3E%3C%2Fspeak%3E?textType=ssml&language" +
      "=en&access_token=pk.XXX");
    urlsCached.add("https://api.mapbox.com/voice/v1/speak/%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody" +
      "%20rate=%221.08%22%3EIn%20200%20meters," +
      "%20enter%20the%20traffic%20circle%20and%20take%20the%204th%20exit%20onto%20Rhode%20Island%20Avenue%20Northwest" +
      "%3C%2Fprosody%3E%3C%2Famazon:effect%3E%3C%2Fspeak%3E?textType=ssml&language=en&access_token=pk.XXX");
    urlsCached.add("https://api.mapbox.com/voice/v1/speak/%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody" +
      "%20rate=%221.08%22%3ETurn%20right%20onto%20Rhode%20Island%20Avenue%20Northwest%3C%2Fprosody%3E%3C%2Famazon" +
      ":effect%3E%3C%2Fspeak%3E?textType=ssml&language=en&access_token=pk.XXX");
    urlsCached.add("https://api.mapbox.com/voice/v1/speak/%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody" +
      "%20rate=%221.08%22%3EIn%20300%20meters," +
      "%20turn%20right%20onto%20Rhode%20Island%20Avenue%20Northwest%3C%2Fprosody%3E%3C%2Famazon:effect%3E%3C%2Fspeak" +
      "%3E?textType=ssml&language=en&access_token=pk.XXX");
    return urlsCached;
  }

}
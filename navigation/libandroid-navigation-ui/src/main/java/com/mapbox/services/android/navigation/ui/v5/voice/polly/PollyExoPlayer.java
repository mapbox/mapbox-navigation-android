package com.mapbox.services.android.navigation.ui.v5.voice.polly;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;

import timber.log.Timber;

public class PollyExoPlayer implements InstructionPlayer {

  private AmazonPollyPresigningClient pollyClient;
  private ExoPlayer exoPlayer;
  private DefaultDataSourceFactory defaultDataSourceFactory;
  private boolean isMuted;

  public PollyExoPlayer(Context context, String awsPoolId) {
    initPollyClient(context, awsPoolId);
    initExoPlayer(context);
  }

  @Override
  public void play(String instruction) {
    if (!isMuted) {
      executeInstructionTask(instruction);
    }
  }

  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  private void initExoPlayer(Context context) {
    DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context,
      null,
      DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
    TrackSelector trackSelector = new DefaultTrackSelector();
    exoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
    exoPlayer.setPlayWhenReady(true);
    String userAgent = Util.getUserAgent(context, "mapboxNavigationUi");
    defaultDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
  }

  private void initPollyClient(Context context, String awsPoolId) {
    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
      context,
      awsPoolId,
      Regions.US_EAST_1
    );
    pollyClient = new AmazonPollyPresigningClient(credentialsProvider);
  }

  private void executeInstructionTask(String instruction) {
    new InstructionTask(pollyClient, new InstructionTask.TaskListener() {
      @Override
      public void onFinished(String speechUrl) {
        {
          Timber.d("Instruction Task Finished: " + speechUrl);
          exoPlayer.prepare(createMediaSource(speechUrl));
        }
      }
    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, instruction);
  }

  private MediaSource createMediaSource(String instructionUrl) {
    return new ExtractorMediaSource(
      Uri.parse(instructionUrl),
      defaultDataSourceFactory,
      new DefaultExtractorsFactory(),
      null,
      null);
  }
}

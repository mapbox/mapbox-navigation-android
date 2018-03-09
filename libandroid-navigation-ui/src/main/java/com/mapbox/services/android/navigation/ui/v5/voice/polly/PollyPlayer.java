package com.mapbox.services.android.navigation.ui.v5.voice.polly;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionListener;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.v5.navigation.MapboxSpeech;
import com.mapbox.services.android.navigation.v5.navigation.VoiceInstructionLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * <p>
 * Will retrieve synthesized speech mp3s from Amazon's AWS Polly Service
 * (Requires a valid AWS Cognito Pool ID)
 * </p><p>
 * Will queue each instruction and play them
 * sequentially up until the queue is empty.
 * </p>
 */
public class PollyPlayer implements InstructionPlayer, Callback<ResponseBody> {
  VoiceInstructionLoader voiceInstructionLoader;
  private MediaPlayer pollyMediaPlayer;
  private List<String> instructionUrls = new ArrayList<>();
  private InstructionListener instructionListener;
  private boolean isMuted;
  private String cacheDirectory;

  /**
   * Construct an instance of {@link PollyPlayer}
   *
   * @param context   to initialize {@link CognitoCachingCredentialsProvider} and {@link AudioManager}
   */
  public PollyPlayer(Context context, Locale locale) {
    this.cacheDirectory = context.getCacheDir().toString();
    voiceInstructionLoader = VoiceInstructionLoader.getInstance();
    voiceInstructionLoader.initialize(
      MapboxSpeech.builder()
        .textType("ssml")
        .language(locale.toString())
        .cacheDirectory(context.getCacheDir())
        .accessToken(Mapbox.getAccessToken()));
  }

  /**
   * @param instruction voice instruction to be synthesized and played.
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      getVoiceFile(instruction);
    }
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    mutePolly(isMuted);
  }

  @Override
  public void onOffRoute() {
    pauseInstruction();
    clearInstructionUrls();
  }

  @Override
  public void onDestroy() {
    stopPollyMediaPlayerPlaying();
  }

  @Override
  public void addInstructionListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  private void mutePolly(boolean isMuted) {
    if (isMuted) {
      stopPollyMediaPlayerPlaying();
      clearInstructionUrls();
    }
  }

  private void stopPollyMediaPlayerPlaying() {
    try {
      if (pollyMediaPlayer != null && pollyMediaPlayer.isPlaying()) {
        pollyMediaPlayer.stop();
        pollyMediaPlayer.release();
        instructionListener.onDone();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void getVoiceFile(final String instruction) {
    voiceInstructionLoader.getInstruction(instruction, this);
  }

  /**
   * Saves the file returned in the response body as a file in the cache directory
   * @param responseBody containing file
   * @return resulting file, or null if there were any IO exceptions
   */
  private File saveAsFile(ResponseBody responseBody) {
    try {
      File file = new File(cacheDirectory + File.separator + "instruction.mp3");
      InputStream inputStream = null;
      OutputStream outputStream = null;

      try {
        inputStream = responseBody.byteStream();
        outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int numOfBytes;

        while ((numOfBytes = inputStream.read(buffer)) != -1) { // -1 denotes end of file
          outputStream.write(buffer, 0, numOfBytes);
        }

        outputStream.flush();
        return file;

      } catch (IOException exception) {
        return null;

      } finally {
        if (inputStream != null) {
          inputStream.close();
        }

        if (outputStream != null) {
          outputStream.close();
        }
      }

    } catch (IOException exception) {
      return null;
    }
  }

  private void playInstruction(String instruction) {
    if (!TextUtils.isEmpty(instruction)) {
      pollyMediaPlayer = new MediaPlayer();
      setDataSource(instruction);
      pollyMediaPlayer.prepareAsync();
      setListeners();
    }
  }

  private void pauseInstruction() {
    try {
      if (pollyMediaPlayer != null && pollyMediaPlayer.isPlaying()) {
        pollyMediaPlayer.stop();
      }
    } catch (IllegalStateException exception) {
      Timber.e(exception.getMessage());
    }
  }

  private void setDataSource(String instruction) {
    try {
      pollyMediaPlayer.setDataSource(instruction);
    } catch (IOException ioException) {
      Timber.e("Unable to set data source for the media pollyMediaPlayer! %s",
        ioException.getMessage());
    }
  }

  private void setListeners() {
    pollyMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        if (instructionListener != null) {
          instructionListener.onStart();
        }
        mp.start();
      }
    });
    pollyMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        mp.release();
        if (instructionListener != null) {
          instructionListener.onDone();
        }
        onInstructionFinished();
      }
    });
  }

  private void onInstructionFinished() {
    if (instructionUrls.size() > 0) {
      instructionUrls.remove(0);
      if (instructionUrls.size() > 0) {
        playInstruction(instructionUrls.get(0));
      }
    }
  }

  private void clearInstructionUrls() {
    if (instructionUrls.size() > 0) {
      instructionUrls.clear();
    }
  }

  @Override
  public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
    if (response.isSuccessful()) {
      String instructionUrl = saveAsFile(response.body()).getPath();

      if (instructionUrls.size() == 0) {
        instructionUrls.add(instructionUrl);
        playInstruction(instructionUrls.get(0));

      } else {
        instructionUrls.add(instructionUrl);
      }
    }
  }

  @Override
  public void onFailure(Call<ResponseBody> call, Throwable throwable) {
    if (instructionListener != null) {
      instructionListener.onError();
    }
  }
}

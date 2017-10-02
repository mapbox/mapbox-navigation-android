package com.mapbox.services.android.navigation.ui.v5.voice.polly;

import android.os.AsyncTask;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.VoiceId;

public class InstructionTask extends AsyncTask<String, Void, Void> {

  private AmazonPollyPresigningClient client;
  private TaskListener listener;

  InstructionTask(AmazonPollyPresigningClient client, TaskListener listener) {
    this.client = client;
    this.listener = listener;
  }

  @Override
  protected Void doInBackground(String... strings) {
    listener.onFinished(retrieveAudioUrl(strings[0]));
    return null;
  }

  private String retrieveAudioUrl(String instruction) {
    SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
      new SynthesizeSpeechPresignRequest()
        .withText(instruction)
        .withVoiceId(VoiceId.Joanna)
        .withOutputFormat(OutputFormat.Mp3);
    try {
      return client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest).toString();
    } catch (AmazonClientException exception) {
      return null;
    }
  }

  public interface TaskListener {
    void onFinished(String speechUrl);
  }
}

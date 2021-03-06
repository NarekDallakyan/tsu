package social.tsu.trimmer.features.compress;

import android.content.Context;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import social.tsu.trimmer.interfaces.VideoCompressListener;

public class VideoCompressor {

  public static void compress(Context context, String inputFile, String outputFile, final VideoCompressListener callback) {
    String cmd = "-threads 2 -y -i " + inputFile + " -strict -2 -vcodec libx264 -preset ultrafast -crf 28 -acodec copy -ac 2 " + outputFile;
    String[] command = cmd.split(" ");
    try {
      FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {
        @Override
        public void onFailure(String msg) {
          if (callback != null) {
            callback.onFailure("Compress video failed!");
            callback.onFinish();
          }
        }

        @Override
        public void onSuccess(String msg) {
          if (callback != null) {
            callback.onSuccess("Compress video successed!");
            callback.onFinish();
          }
        }
      });
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}

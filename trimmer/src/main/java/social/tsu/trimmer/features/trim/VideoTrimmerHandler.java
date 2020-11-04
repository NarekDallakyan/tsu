package social.tsu.trimmer.features.trim;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import social.tsu.trimmer.interfaces.VideoTrimListener;
import social.tsu.trimmer.widget.VideoTrimmerView;


public class VideoTrimmerHandler implements VideoTrimListener {

  private static final String VIDEO_PATH_KEY = "video-file-path";
  public static final int VIDEO_TRIM_REQUEST_CODE = 0x001;
  private VideoTrimmerView trimmerView;
  private OnTrimComplete callback;

  public static void call(FragmentActivity from, String videoPath) {

    if (!TextUtils.isEmpty(videoPath)) {
      Bundle bundle = new Bundle();
      bundle.putString(VIDEO_PATH_KEY, videoPath);
      Intent intent = new Intent(from, VideoTrimmerHandler.class);
      intent.putExtras(bundle);
      from.startActivityForResult(intent, VIDEO_TRIM_REQUEST_CODE);
    }
  }

  public void initUI(VideoTrimmerView trimmerView, String path, Context context) {
    this.trimmerView = trimmerView;
    trimmerView.setOnTrimVideoListener(this);
    trimmerView.initVideoByURI(Uri.parse(path));
  }

  public long getVideoTrimDuration() {
    return trimmerView.getVideoTrimDuration();
  }


  public void onPause() {
    trimmerView.onVideoPause();
    trimmerView.setRestoreState(true);
  }

  public void onDestroy() {
    trimmerView.onDestroy();
  }

  public void onStartTrim() {

  }

  @Override
  public void onFinishTrim(String in) {
    callback.onComplete(in);
  }

  @Override
  public void onCancel() {
    trimmerView.onDestroy();
  }

  public void onSave(OnTrimComplete callback) {
    this.callback = callback;
    trimmerView.onSaveClicked();
  }

  public interface OnTrimComplete {
    void onComplete(String filePath);
  }
}

package social.tsu.trimmer.features.trim;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.FragmentActivity;

import iknow.android.utils.BaseUtils;
import social.tsu.trimmer.interfaces.VideoTrimListener;
import social.tsu.trimmer.widget.VideoTrimmerView;


public class VideoTrimmerHandler implements VideoTrimListener {

  private static final String VIDEO_PATH_KEY = "video-file-path";
  public static final int VIDEO_TRIM_REQUEST_CODE = 0x001;
  private ProgressDialog mProgressDialog;
  private VideoTrimmerView trimmerView;
  private Context context;
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

  private static void initFFmpegBinary(Context context) {

  }

  public static void initContext(Context context) {

    BaseUtils.init(context);
    initFFmpegBinary(context);
  }

  public void initUI(VideoTrimmerView trimmerView, String path, Context context) {
    this.trimmerView = trimmerView;
    this.context = context;

    trimmerView.setOnTrimVideoListener(this);
    trimmerView.initVideoByURI(Uri.parse(path));
  }


  public void onPause() {
    trimmerView.onVideoPause();
    trimmerView.setRestoreState(true);
  }

  public void onDestroy() {
    trimmerView.onDestroy();
  }

  public void onStartTrim() {
    buildDialog("trimming video...").show();
  }

  @Override
  public void onFinishTrim(String in) {
    if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
    callback.onComplete(in);
  }

  @Override
  public void onCancel() {
    trimmerView.onDestroy();
  }

  private ProgressDialog buildDialog(String msg) {
    if (mProgressDialog == null) {
      mProgressDialog = ProgressDialog.show(context, "", msg);
    }
    mProgressDialog.setMessage(msg);
    return mProgressDialog;
  }

  public void onSave(OnTrimComplete callback) {
    this.callback = callback;
    trimmerView.onSaveClicked();
  }

  public interface OnTrimComplete {
    void onComplete(String filePath);
  }
}

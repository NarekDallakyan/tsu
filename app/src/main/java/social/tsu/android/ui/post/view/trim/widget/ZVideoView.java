package social.tsu.android.ui.post.view.trim.widget;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;


public class ZVideoView extends VideoView {
  private Context context;

  public ZVideoView(Context context) {
    super(context);
    this.context = context;
  }

  public ZVideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
  }

  public ZVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.context = context;
  }

  @Override
  public void setVideoURI(Uri uri) {
    super.setVideoURI(uri);
  }
}

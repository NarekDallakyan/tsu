package social.tsu.android.ui.post.view.trim.interfaces;

public interface VideoTrimListener {
    void onStartTrim();
    void onFinishTrim(String url);
    void onCancel();
}

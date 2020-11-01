package social.tsu.trimmer.interfaces;

import android.net.Uri;

public interface OnTrimVideoListener {

    void onTrimResult(final Uri uri);

    void onTrimCancel();
}

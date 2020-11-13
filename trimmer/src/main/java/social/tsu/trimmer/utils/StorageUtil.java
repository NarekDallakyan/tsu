package social.tsu.trimmer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import iknow.android.utils.BaseUtils;
import iknow.android.utils.BuildConfig;


@SuppressWarnings({"ResultOfMethodCallIgnored", "FieldCanBeLocal"})
public class StorageUtil {

    private static final String TAG = "StorageUtil";
    private static String APP_DATA_PATH = "/Android/data/" + BuildConfig.APPLICATION_ID;
    private static String sDataDir;
    private static String sCacheDir;

    public static String getAppDataDir() {
        if (TextUtils.isEmpty(sDataDir)) {
            try {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    sDataDir = Environment.getExternalStorageDirectory().getPath() + APP_DATA_PATH;
                    if (TextUtils.isEmpty(sDataDir)) {
                        sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
                    }
                } else {
                    sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
            }
            File file = new File(sDataDir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return sDataDir;
    }

    public static String getCacheDir() {
        if (TextUtils.isEmpty(sCacheDir)) {
            File file = null;
            Context context = BaseUtils.getContext();
            try {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    file = context.getExternalCacheDir();
                    if (file == null || !file.exists()) {
                        file = getExternalCacheDirManual(context);
                    }
                }
                if (file == null) {
                    file = context.getCacheDir();
                    if (file == null || !file.exists()) {
                        file = getCacheDirManual(context);
                    }
                }
                Log.w(TAG, "cache dir = " + file.getAbsolutePath());
                sCacheDir = file.getAbsolutePath();
            } catch (Throwable ignored) {
            }
        }
        return sCacheDir;
    }

    private static File getExternalCacheDirManual(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {//
                Log.w(TAG, "Unable to create external cache directory");
                return null;
            }
            try {
                new File(appCacheDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                Log.i(TAG, "Can't create \".nomedia\" file in application external cache directory");
            }
        }
        return appCacheDir;
    }

    @SuppressLint("SdCardPath")
    private static File getCacheDirManual(Context context) {
        String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache";
        return new File(cacheDirPath);
    }
}

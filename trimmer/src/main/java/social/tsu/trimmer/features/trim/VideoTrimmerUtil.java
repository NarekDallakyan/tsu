package social.tsu.trimmer.features.trim;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import iknow.android.utils.DeviceUtil;
import iknow.android.utils.UnitConverter;
import iknow.android.utils.callback.SingleCallback;
import iknow.android.utils.thread.BackgroundExecutor;
import social.tsu.trimmer.interfaces.VideoTrimListener;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static social.tsu.trimmer.widget.VideoTrimmerView.getMaxDuration;

public class VideoTrimmerUtil {

    public static final long MIN_SHOOT_DURATION = 1000L;
    public static final int MAX_COUNT_RANGE = 10;
    public static final int RECYCLER_VIEW_PADDING = UnitConverter.dpToPx(35);
    private static final int SCREEN_WIDTH_FULL = DeviceUtil.getDeviceWidth();
    public static final int VIDEO_FRAMES_WIDTH = SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2;
    private static final int THUMB_HEIGHT = UnitConverter.dpToPx(50);
    public static long TYPE = -1;
    public static int VIDEO_MAX_TIME = 7200;
    public static final long MAX_SHOOT_DURATION_VIDEO = VIDEO_MAX_TIME * 1000L;
    public static int GIF_MAX_TIME = 7200;
    public static final long MAX_SHOOT_DURATION_GIF = GIF_MAX_TIME * 1000L;
    public static final int THUMB_WIDTH = (SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2) / getMaxDuration();

    public static void trim(Context context, String inputFile, String outputFile, long startMs, long endMs, final VideoTrimListener callback) {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String outputName = "trimmedVideo_" + timeStamp + ".mp4";
        outputFile = outputFile + "/" + outputName;

        String start = convertSecondsToTime(startMs / 1000);
        String duration = convertSecondsToTime((endMs - startMs) / 1000);

        String cmd = "-ss " + start + " -t " + duration + " -accurate_seek" + " -i " + inputFile + " -codec copy -avoid_negative_ts 1 " + outputFile;

        String[] command = cmd.split(" ");
        callback.onStartTrim();
        try {
            final String tempOutFile = outputFile;
            int result = FFmpeg.execute(command);
            if (result == RETURN_CODE_SUCCESS) {
                callback.onFinishTrim(tempOutFile);
            } else if (result == RETURN_CODE_CANCEL) {
                callback.onCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shootVideoThumbInBackground(final Context context, final Uri videoUri, final int totalThumbsCount, final long startPosition,
                                                   final long endPosition, final SingleCallback<Bitmap, Integer> callback) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
            @Override
            public void execute() {
                try {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(context, videoUri);
                    // Retrieve media data use microsecond
                    long interval = (endPosition - startPosition) / (totalThumbsCount - 1);
                    for (long i = 0; i < totalThumbsCount; ++i) {
                        long frameTime = startPosition + interval * i;
                        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(frameTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if (bitmap == null) continue;
                        try {
                            bitmap = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, THUMB_HEIGHT, false);
                        } catch (final Throwable t) {
                            t.printStackTrace();
                        }
                        callback.onSingleCallback(bitmap, (int) interval);
                    }
                    mediaMetadataRetriever.release();
                } catch (final Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    private static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0) {
            return "00:00";
        } else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }
}

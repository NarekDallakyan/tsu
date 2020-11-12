package social.tsu.overlay.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import social.tsu.overlay.R;
import social.tsu.overlay.photoeditor.OnPhotoEditorListener;
import social.tsu.overlay.photoeditor.PhotoEditor;
import social.tsu.overlay.photoeditor.PhotoEditorView;
import social.tsu.overlay.photoeditor.SaveSettings;
import social.tsu.overlay.photoeditor.TextStyleBuilder;
import social.tsu.overlay.photoeditor.ViewType;
import social.tsu.overlay.utils.DimensionData;

import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static social.tsu.overlay.utils.Utils.getScaledDimension;

public class OverlayHandler implements OnPhotoEditorListener {

    private static final String TAG = OverlayHandler.class.getSimpleName();
    private PhotoEditor mPhotoEditor;
    private String globalVideoUrl = "";
    private MediaPlayer mediaPlayer;
    private String videoPath = "";
    private String imagePath = "";
    private ArrayList<String> exeCmd;
    private String[] newCommand;
    private int originalDisplayWidth;
    private int originalDisplayHeight;
    private int newCanvasWidth, newCanvasHeight;
    private int DRAW_CANVASW = 0;
    private int DRAW_CANVASH = 0;

    private MediaMetadataRetriever retriever;
    private MediaPlayer.OnCompletionListener onCompletionListener = mediaPlayer -> mediaPlayer.start();

    private Context context;
    private PhotoEditorView photoEditorView;
    private TextureView videoSurface;
    private EditText mAddTextEditText;
    private AppCompatActivity appCompatActivity;
    private int mColorCode;
    private OverlayListener listener;

    public void destroy() {

        if (mediaPlayer == null) return;
        mediaPlayer.stop();
    }

    public void initialize(
            AppCompatActivity appCompatActivity,
            Context context,
            PhotoEditorView photoEditorView,
            TextureView videoSurface,
            EditText mAddTextEditText
    ) {
        this.mAddTextEditText = mAddTextEditText;
        this.appCompatActivity = appCompatActivity;
        this.context = context;
        this.photoEditorView = photoEditorView;
        this.videoSurface = videoSurface;
    }

    public void onCreate(String path) {
        initViews();
        Glide.with(context).load(R.drawable.trans).centerCrop().into(photoEditorView.getSource());
        videoPath = path;
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoPath);
        String metaRotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION);
        int rotation = metaRotation == null ? 0 : Integer.parseInt(metaRotation);
        if (rotation == 90 || rotation == 270) {
            DRAW_CANVASH = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            DRAW_CANVASW = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        } else {
            DRAW_CANVASW = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            DRAW_CANVASH = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        }
        setCanvasAspectRatio();

        videoSurface.getLayoutParams().width = newCanvasWidth;
        videoSurface.getLayoutParams().height = newCanvasHeight;

        photoEditorView.getLayoutParams().width = newCanvasWidth;
        photoEditorView.getLayoutParams().width = newCanvasWidth;
    }

    private void initViews() {
        mPhotoEditor = new PhotoEditor.Builder(context, photoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                .build(); // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this);

        videoSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);

                try {
                    mediaPlayer = new MediaPlayer();
                    Log.d("VideoPath>>", videoPath);
                    mediaPlayer.setDataSource(videoPath);
                    mediaPlayer.setSurface(surface);
                    mediaPlayer.prepare();
                    mediaPlayer.setOnCompletionListener(onCompletionListener);
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.start();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });

        exeCmd = new ArrayList<>();
    }

    public void executeCommand(String[] command, final String absolutePath) {

        try {
            int commandResult = FFmpeg.execute(command);
            if (commandResult == RETURN_CODE_SUCCESS) {
                listener.onSave(absolutePath);
            } else {
                listener.onError();
            }
        } catch (Exception error) {
            listener.onError();
        }
    }

    private void setCanvasAspectRatio() {

        originalDisplayHeight = getDisplayHeight();
        originalDisplayWidth = getDisplayWidth();

        DimensionData displayDiamenion =
                getScaledDimension(new DimensionData(DRAW_CANVASW, DRAW_CANVASH),
                        new DimensionData(originalDisplayWidth, originalDisplayHeight));
        newCanvasWidth = displayDiamenion.width;
        newCanvasHeight = displayDiamenion.height;

    }

    @SuppressLint("MissingPermission")
    public void saveOverlay(OverlayListener listener) {
        this.listener = listener;
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + ""
                + System.currentTimeMillis() + ".png");
        try {
            file.createNewFile();

            SaveSettings saveSettings = new SaveSettings.Builder()
                    .setClearViewsEnabled(true)
                    .setTransparencyEnabled(false)
                    .build();

            mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                @Override
                public void onSuccess(@NonNull String imagePath) {
                    OverlayHandler.this.imagePath = imagePath;
                    applayWaterMark();
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    listener.onError();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            listener.onError();
        }
    }

    private void applayWaterMark() {

        File output = new File(Environment.getExternalStorageDirectory()
                + File.separator + ""
                + System.currentTimeMillis() + ".mp4");
        try {
            output.createNewFile();

            exeCmd.add("-y");
            exeCmd.add("-i");
            exeCmd.add(videoPath);
            exeCmd.add("-i");
            exeCmd.add(imagePath);
            exeCmd.add("-filter_complex");
            exeCmd.add("[1:v]scale=" + DRAW_CANVASW + ":" + DRAW_CANVASH + "[ovrl];[0:v][ovrl]overlay=x=0:y=0");
            exeCmd.add("-c:v");
            exeCmd.add("libx264");
            exeCmd.add("-preset");
            exeCmd.add("ultrafast");
            exeCmd.add(output.getAbsolutePath());


            newCommand = new String[exeCmd.size()];
            for (int j = 0; j < exeCmd.size(); j++) {
                newCommand[j] = exeCmd.get(j);
            }

            executeCommand(newCommand, output.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode, final int position) {
    }

    public String generatePath(Uri uri, Context context) {
        String filePath = null;
        final boolean isKitKat = true;
        if (isKitKat) {
            filePath = generateFromKitkat(uri, context);
        }

        if (filePath != null) {
            return filePath;
        }

        Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath == null ? uri.getPath() : filePath;
    }

    @TargetApi(19)
    private String generateFromKitkat(Uri uri, Context context) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String wholeID = DocumentsContract.getDocumentId(uri);

            String id = wholeID.split(":")[1];

            String[] column = {MediaStore.Video.Media.DATA};
            String sel = MediaStore.Video.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().
                    query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            column, sel, new String[]{id}, null);


            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            cursor.close();
        }
        return filePath;
    }

    private int getDisplayWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        appCompatActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    private int getDisplayHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        appCompatActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    public void colorItemClicked(int color, String name, boolean isWaterMarkOn) {
        mColorCode = color;

        boolean colorIsWhite = name.toLowerCase().equals("white");

        if (isWaterMarkOn) {

            if (colorIsWhite) {
                mAddTextEditText.setTextColor(Color.BLACK);
            } else {
                mAddTextEditText.setTextColor(Color.WHITE);
            }
            mAddTextEditText.setBackgroundColor(color);
        } else {
            mAddTextEditText.setTextColor(mColorCode);
        }
    }

    public void fontItemClicked(Typeface font) {

        mAddTextEditText.setTypeface(font);
    }

    public void watermark(boolean on, int color, String name) {

        boolean colorIsWhite = name.toLowerCase().equals("white");

        if (!on) {
            mAddTextEditText.setBackgroundColor(Color.TRANSPARENT);
            mAddTextEditText.setTextColor(color);
        } else {
            mAddTextEditText.setBackgroundColor(color);
            if (colorIsWhite) {
                mAddTextEditText.setTextColor(Color.BLACK);
            } else {
                mAddTextEditText.setTextColor(Color.WHITE);
            }
        }
    }

    public void onDoneClicked(Typeface typeface, int color, String name, boolean isWatermarkOn, String text) {

        final TextStyleBuilder styleBuilder = new TextStyleBuilder();
        boolean colorIsWhite = name.toLowerCase().equals("white");
        styleBuilder.withTextFont(typeface);
        if (!isWatermarkOn) {
            styleBuilder.withTextColor(color);
        } else {
            styleBuilder.withBackgroundColor(color);
            if (colorIsWhite) {
                styleBuilder.withTextColor(Color.BLACK);
            } else {
                styleBuilder.withTextColor(Color.WHITE);
            }
        }
        mPhotoEditor.addText(text, styleBuilder, 0);
    }

    public void changeTextGravity(int gravity) {
        if (gravity == 0) {
            mAddTextEditText.setGravity(Gravity.START);
        } else if (gravity == 1) {
            mAddTextEditText.setGravity(Gravity.CENTER);
        } else if (gravity == 2) {
            mAddTextEditText.setGravity(Gravity.END);
        }
    }

    public void release(Typeface defaultTypeface) {

        mAddTextEditText.setBackgroundColor(Color.TRANSPARENT);
        mAddTextEditText.setTextColor(Color.WHITE);
        mAddTextEditText.setTypeface(defaultTypeface);
        mAddTextEditText.setText("");
    }

    public interface OverlayListener {

        void onSave(String filePath);

        void onError();
    }
}

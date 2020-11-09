package social.tsu.overlay;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

import social.tsu.overlay.photoeditor.OnPhotoEditorListener;
import social.tsu.overlay.photoeditor.PhotoEditor;
import social.tsu.overlay.photoeditor.PhotoEditorView;
import social.tsu.overlay.photoeditor.SaveSettings;
import social.tsu.overlay.photoeditor.TextStyleBuilder;
import social.tsu.overlay.photoeditor.ViewType;


public class PreviewPhotoActivity extends AppCompatActivity implements OnPhotoEditorListener, PropertiesBSFragment.Properties,
        View.OnClickListener,
        StickerBSFragment.StickerListener {

    private static final String TAG = PreviewPhotoActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private PropertiesBSFragment propertiesBSFragment;
    private StickerBSFragment mStickerBSFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);
        initViews();
//        Drawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
        Glide.with(this).load(getIntent().getStringExtra("DATA")).into(((PhotoEditorView) findViewById(R.id.ivImage)).getSource());
//        Glide.with(this).load(R.drawable.trans).into(binding.ivImage.getSource());
    }

    private void initViews() {
        mStickerBSFragment = new StickerBSFragment();
        mStickerBSFragment.setStickerListener(this);
        propertiesBSFragment = new PropertiesBSFragment();
        propertiesBSFragment.setPropertiesChangeListener(this);
        mPhotoEditor = new PhotoEditor.Builder(this, findViewById(R.id.ivImage))
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                .setDeleteView(findViewById(R.id.imgDelete))
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build(); // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this);


        findViewById(R.id.imgClose).setOnClickListener(this);
        findViewById(R.id.imgDone).setOnClickListener(this);
        findViewById(R.id.imgDraw).setOnClickListener(this);
        findViewById(R.id.imgText).setOnClickListener(this);
        findViewById(R.id.imgUndo).setOnClickListener(this);
        findViewById(R.id.imgSticker).setOnClickListener(this);

        if (mPhotoEditor.undoCanvas()) {

        }

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgClose) {
            onBackPressed();
        } else if (id == R.id.imgDone) {
            saveImage();
        } else if (id == R.id.imgDraw) {
            setDrawingMode();
        } else if (id == R.id.imgText) {
            TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(PreviewPhotoActivity.this, 0);
            textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {

                @Override
                public void onDone(String inputText, int colorCode, int position) {
                    final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                    styleBuilder.withTextColor(colorCode);
                    Typeface typeface = ResourcesCompat.getFont(PreviewPhotoActivity.this, TextEditorDialogFragment.getDefaultFontIds(PreviewPhotoActivity.this).get(position));
                    styleBuilder.withTextFont(typeface);
                    mPhotoEditor.addText(inputText, styleBuilder, position);
                }
            });
        } else if (id == R.id.imgUndo) {
            Log.d("canvas>>", mPhotoEditor.undoCanvas() + "");
            mPhotoEditor.clearBrushAllViews();
        } else if (id == R.id.imgSticker) {
            mStickerBSFragment.show(getSupportFragmentManager(), mStickerBSFragment.getTag());
        }
    }

    private void setDrawingMode() {
        if (mPhotoEditor.getBrushDrawableMode()) {
            mPhotoEditor.setBrushDrawingMode(false);
            findViewById(R.id.imgDraw).setBackgroundColor(ContextCompat.getColor(this, R.color.black_trasp));
        } else {
            mPhotoEditor.setBrushDrawingMode(true);
            findViewById(R.id.imgDraw).setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            propertiesBSFragment.show(getSupportFragmentManager(), propertiesBSFragment.getTag());
        }
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {

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
                    ((PhotoEditorView) findViewById(R.id.ivImage)).getSource().setImageURI(Uri.fromFile(new File(imagePath)));
                    Toast.makeText(PreviewPhotoActivity.this, "Saved successfully...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(PreviewPhotoActivity.this, "Saving Failed...", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST:
                    mPhotoEditor.clearAllViews();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mPhotoEditorView.getSource().setImageBitmap(photo);
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    public void onStickerClick(Bitmap bitmap) {
        mPhotoEditor.setBrushDrawingMode(false);
        findViewById(R.id.imgDraw).setBackgroundColor(ContextCompat.getColor(this, R.color.black_trasp));
        mPhotoEditor.addImage(bitmap);
    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode, final int position) {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode, position);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode, int position) {
                final TextStyleBuilder styleBuilder = new TextStyleBuilder();
                styleBuilder.withTextColor(colorCode);
                Typeface typeface = ResourcesCompat.getFont(PreviewPhotoActivity.this, TextEditorDialogFragment.getDefaultFontIds(PreviewPhotoActivity.this).get(position));
                styleBuilder.withTextFont(typeface);
                mPhotoEditor.editText(rootView, inputText, styleBuilder, position);
            }
        });
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

    @Override
    public void onColorChanged(int colorCode) {
        mPhotoEditor.setBrushColor(colorCode);
    }

    @Override
    public void onOpacityChanged(int opacity) {

    }

    @Override
    public void onBrushSizeChanged(int brushSize) {

    }

}

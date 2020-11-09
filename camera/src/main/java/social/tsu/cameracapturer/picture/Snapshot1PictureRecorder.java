package social.tsu.cameracapturer.picture;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

import social.tsu.cameracapturer.camera.CameraLogger;
import social.tsu.cameracapturer.camera.PictureResult;
import social.tsu.cameracapturer.engine.Camera1Engine;
import social.tsu.cameracapturer.engine.offset.Reference;
import social.tsu.cameracapturer.internal.utils.CropHelper;
import social.tsu.cameracapturer.internal.utils.RotationHelper;
import social.tsu.cameracapturer.internal.utils.WorkerHandler;
import social.tsu.cameracapturer.size.AspectRatio;
import social.tsu.cameracapturer.size.Size;

/**
 * A {@link PictureRecorder} that uses standard APIs.
 */
public class Snapshot1PictureRecorder extends PictureRecorder {

    private static final String TAG = Snapshot1PictureRecorder.class.getSimpleName();
    @SuppressWarnings("unused")
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera1Engine mEngine1;
    private Camera mCamera;
    private AspectRatio mOutputRatio;
    private int mFormat;

    public Snapshot1PictureRecorder(
            @NonNull PictureResult.Stub stub,
            @NonNull Camera1Engine engine,
            @NonNull Camera camera,
            @NonNull AspectRatio outputRatio) {
        super(stub, engine);
        mEngine1 = engine;
        mCamera = camera;
        mOutputRatio = outputRatio;
        mFormat = camera.getParameters().getPreviewFormat();
    }

    @Override
    public void take() {
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(@NonNull final byte[] yuv, Camera camera) {
                dispatchOnShutter(false);

                // Got to rotate the preview frame, since byte[] data here does not include
                // EXIF tags automatically set by camera. So either we add EXIF, or we rotate.
                // Adding EXIF to a byte array, unfortunately, is hard.
                final int sensorToOutput = mResult.rotation;
                final Size outputSize = mResult.size;
                final Size previewStreamSize = mEngine1.getPreviewStreamSize(Reference.SENSOR);
                if (previewStreamSize == null) {
                    throw new IllegalStateException("Preview stream size should never be null here.");
                }
                WorkerHandler.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Rotate the picture, because no one will write EXIF data,
                        // then crop if needed. In both cases, transform yuv to jpeg.
                        //noinspection deprecation
                        byte[] data = RotationHelper.rotate(yuv, previewStreamSize, sensorToOutput);
                        YuvImage yuv = new YuvImage(data, mFormat, outputSize.getWidth(), outputSize.getHeight(), null);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Rect outputRect = CropHelper.computeCrop(outputSize, mOutputRatio);
                        yuv.compressToJpeg(outputRect, 90, stream);
                        data = stream.toByteArray();

                        mResult.data = data;
                        mResult.size = new Size(outputRect.width(), outputRect.height());
                        mResult.rotation = 0;
                        mResult.format = PictureResult.FORMAT_JPEG;
                        dispatchResult();
                    }
                });

                // It seems that the buffers are already cleared here, so we need to allocate again.
                camera.setPreviewCallbackWithBuffer(null); // Release anything left
                camera.setPreviewCallbackWithBuffer(mEngine1); // Add ourselves
                mEngine1.getFrameManager().setUp(mFormat, previewStreamSize);
            }
        });
    }

    @Override
    protected void dispatchResult() {
        mEngine1 = null;
        mCamera = null;
        mOutputRatio = null;
        mFormat = 0;
        super.dispatchResult();
    }
}

package social.tsu.cameracapturer.picture;

import android.hardware.Camera;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import social.tsu.cameracapturer.camera.CameraLogger;
import social.tsu.cameracapturer.camera.PictureResult;
import social.tsu.cameracapturer.internal.utils.ExifHelper;

/**
 * A {@link PictureResult} that uses standard APIs.
 */
public class Full1PictureRecorder extends PictureRecorder {

    private static final String TAG = Full1PictureRecorder.class.getSimpleName();
    @SuppressWarnings("unused")
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Camera mCamera;

    public Full1PictureRecorder(@NonNull PictureResult.Stub stub,
                                @Nullable PictureResultListener listener,
                                @NonNull Camera camera) {
        super(stub, listener);
        mCamera = camera;

        // We set the rotation to the camera parameters, but we don't know if the result will be
        // already rotated with 0 exif, or original with non zero exif. we will have to read EXIF.
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(mResult.rotation);
        mCamera.setParameters(params);
    }

    @Override
    public void take() {
        mCamera.takePicture(
                new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        dispatchOnShutter(true);
                    }
                },
                null,
                null,
                new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {
                        int exifRotation;
                        try {
                            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(data));
                            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            exifRotation = ExifHelper.readExifOrientation(exifOrientation);
                        } catch (IOException e) {
                            exifRotation = 0;
                        }
                        mResult.format = PictureResult.FORMAT_JPEG;
                        mResult.data = data;
                        mResult.rotation = exifRotation;
                        camera.startPreview(); // This is needed, read somewhere in the docs.
                        dispatchResult();
                    }
                }
        );
    }

    @Override
    protected void dispatchResult() {
        mCamera = null;
        super.dispatchResult();
    }
}

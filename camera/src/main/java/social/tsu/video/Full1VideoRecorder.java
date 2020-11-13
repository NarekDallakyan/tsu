package social.tsu.video;

import android.hardware.Camera;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;

import social.tsu.camera.CameraLogger;
import social.tsu.camera.VideoResult;
import social.tsu.engine.Camera1Engine;
import social.tsu.internal.utils.CamcorderProfiles;
import social.tsu.size.Size;

/**
 * A {@link VideoRecorder} that uses {@link MediaRecorder} APIs
 * for the Camera1 engine.
 */
public class Full1VideoRecorder extends FullVideoRecorder {

    private static final String TAG = Full1VideoRecorder.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private final Camera1Engine mEngine;
    private final Camera mCamera;
    private final int mCameraId;

    public Full1VideoRecorder(@NonNull Camera1Engine engine,
                              @NonNull Camera camera, int cameraId) {
        super(engine);
        mCamera = camera;
        mEngine = engine;
        mCameraId = cameraId;
    }

    @Override
    protected boolean onPrepareMediaRecorder(@NonNull VideoResult.Stub stub, @NonNull MediaRecorder mediaRecorder) {
        mediaRecorder.setCamera(mCamera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Get a profile of quality compatible with the chosen size.
        Size size = stub.rotation % 180 != 0 ? stub.size.flip() : stub.size;
        mProfile = CamcorderProfiles.get(mCameraId, size);
        return super.onPrepareMediaRecorder(stub, mediaRecorder);
    }

    @Override
    protected void dispatchResult() {
        // Restore frame processing.
        mCamera.setPreviewCallbackWithBuffer(mEngine);
        super.dispatchResult();
    }
}

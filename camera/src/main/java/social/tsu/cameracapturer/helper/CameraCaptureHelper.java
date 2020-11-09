package social.tsu.cameracapturer.helper;

import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import social.tsu.cameracapturer.camera.CameraListener;
import social.tsu.cameracapturer.camera.CameraView;
import social.tsu.cameracapturer.control.Flash;
import social.tsu.cameracapturer.filter.Filter;

public class CameraCaptureHelper {

    private CameraView cameraView;
    private LifecycleOwner owner;
    private CameraListener cameraListener;

    public void initialize(CameraView cameraView, @NonNull LifecycleOwner owner, @NonNull CameraListener cameraListener) {
        this.cameraView = cameraView;
        this.owner = owner;
        this.cameraListener = cameraListener;
        cameraView.setLifecycleOwner(owner);
        cameraView.addCameraListener(cameraListener);
    }

    public void toggleCamera() {

        if (cameraView == null) return;
        cameraView.toggleFacing();
    }

    public void handleFlash() {

        if (cameraView == null) return;

        if (cameraView.getFlash() == Flash.TORCH) {
            cameraView.setFlash(Flash.OFF);
            return;
        }
        cameraView.setFlash(Flash.TORCH);
    }

    public void capturePicture() {

        if (cameraView == null) return;
        cameraView.takePictureSnapshot();
    }

    public boolean isRecording() {

        return cameraView.isTakingVideo();
    }

    public void startRecording() {

        if (cameraView == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.US);
        String currentTimeStamp = dateFormat.format(new Date());

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "CameraViewFreeDrawing";
        File outputDir = new File(path);
        outputDir.mkdirs();
        File saveTo = new File(path + File.separator + currentTimeStamp + ".mp4");
        cameraView.takeVideoSnapshot(saveTo);
    }

    public void changeFilter(Filter filters) {
        cameraView.setFilter(filters);
    }

    public void onRestart() {

        //cameraView.restart();
    }

    public void onStop() {

        cameraView.stop();
    }
}

package social.tsu.overlay.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.kbeanie.multipicker.api.CameraVideoPicker;
import com.kbeanie.multipicker.api.entity.ChosenImage;
import com.kbeanie.multipicker.api.entity.ChosenVideo;

import java.util.List;

import social.tsu.overlay.R;
import social.tsu.overlay.utils.CameraUtils;

public class OverlayMainActivity extends AppCompatActivity implements CameraUtils.OnCameraResult {

    private CameraUtils cameraUtils;
    private CameraVideoPicker cameraVideoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraUtils = new CameraUtils(this, this);

        findViewById(R.id.btnPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraUtils.openCameraGallery();
            }
        });
        findViewById(R.id.btnVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                cameraUtils.alertVideoSelcetion();


            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        cameraUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onSuccess(List<ChosenImage> images) {
    }

    @Override
    public void onError(String error) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        cameraUtils.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onVideoSuccess(List<ChosenVideo> list) {
        if (list != null && list.size() > 0) {
            Intent i = new Intent(OverlayMainActivity.this, OverlayHandler.class);
            i.putExtra("DATA", list.get(0).getOriginalPath());
            //binding.ivProfilePic.setImageURI(Uri.fromFile(selectedImageFile));
            startActivity(i);

        }
    }
}


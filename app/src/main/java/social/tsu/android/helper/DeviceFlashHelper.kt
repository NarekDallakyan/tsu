package social.tsu.android.helper

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import androidx.annotation.NonNull
import social.tsu.android.TsuApplication

class DeviceFlashHelper {

    companion object {

        var isFlashlightOn = false

        fun registerFlashlightState(context: Context) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.registerTorchCallback(torchCallback, null)
        }

        fun unregisterFlashlightState(context: Context) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.unregisterTorchCallback(torchCallback)
        }

        private val torchCallback: CameraManager.TorchCallback =
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(@NonNull cameraId: String, enabled: Boolean) {
                    super.onTorchModeChanged(cameraId, enabled)
                    isFlashlightOn = enabled
                }
            }

        fun deviceFlashIsAvailable(): Boolean {

            return TsuApplication.mContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        }

        fun switchFlashLight(enable: Boolean) {

            //getting the camera manager and camera id
            val mCameraManager =
                (TsuApplication.mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager)

            try {
                val mCameraId = mCameraManager.cameraIdList[0]
                mCameraManager.setTorchMode(mCameraId, enable)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }
}
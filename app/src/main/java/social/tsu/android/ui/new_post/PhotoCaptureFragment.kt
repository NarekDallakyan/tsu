package social.tsu.android.ui.new_post

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import social.tsu.android.ANIMATION_FAST_MILLIS
import social.tsu.android.ANIMATION_SLOW_MILLIS
import social.tsu.android.ui.CameraUtil
import social.tsu.android.utils.pickMediaFromGallery
import java.io.File


class PhotoCaptureFragment : BaseCameraFragment<ImageCapture>() {

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
    }

    @SuppressLint("RestrictedApi")
    override fun createCapture(aspectRatio: Int, rotation: Int): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
    }

    override fun onCaptureImageClick(button: ImageButton) {
        // Create output file to hold the image
        val photoFile = CameraUtil.createFile(
            outputDirectory,
            FILENAME,
            PHOTO_EXTENSION
        )

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        // Setup image capture listener which is triggered after photo has been taken
        val options = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        capture?.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onImageSaved(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed", exception)
            }
        })

        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Display flash animation to indicate that photo was captured
            cameraContainer.postDelayed({
                cameraContainer.foreground = ColorDrawable(Color.WHITE)
                cameraContainer.postDelayed(
                    { cameraContainer.foreground = null }, ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    override fun pickFromGallery() {
        pickMediaFromGallery(REQUEST_PICK_LIBRARY, "image/*")
    }

    override fun onGetPickResult(data: Intent?) {
        if (data != null) {
            data.data?.let { photoUri ->
                proceedNext(photoUri)
            }
        }
    }

    override fun onDisplayChanged(view: View) {
        capture?.targetRotation = view.display.rotation
    }

    private fun onImageSaved(photoFile: File) {
        Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")

        Log.d(TAG, "file exists ${photoFile.exists()}")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            requireActivity().sendBroadcast(
                Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(photoFile))
            )
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        try {
            MediaScannerConnection.scanFile(
                requireActivity().baseContext,
                arrayOf(photoFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        } catch (e:Exception){
            e.printStackTrace()
        }

        val intent = Intent()
        intent.putExtra(CameraUtil.EXTRA_IMAGE_PATH, photoFile.toString())
        intent.putExtra(
            CameraUtil.EXTRA_CUSTOM_CAMERA_MODE,
            CameraUtil.MODE_CAMERA
        )

        try {
            proceedNext(Uri.parse(photoFile.absolutePath))
        } catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    private fun proceedNext(photoUri: Uri) {
        val fragment = requireParentFragment().requireParentFragment()
        if (fragment is PostTypesFragment) {
            fragment.next(photoUri = photoUri)
        }
    }

}

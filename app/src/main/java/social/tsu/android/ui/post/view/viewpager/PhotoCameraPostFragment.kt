package social.tsu.android.ui.post.view.viewpager

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import social.tsu.android.ANIMATION_FAST_MILLIS
import social.tsu.android.ANIMATION_SLOW_MILLIS
import social.tsu.android.ui.CameraUtil
import social.tsu.android.ui.new_post.BaseCameraFragment
import social.tsu.android.ui.post.view.PostTypesFragment
import social.tsu.android.utils.pickMediaFromGallery


class PhotoCameraPostFragment : BaseCameraFragment<ImageCapture>() {

    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
    }

    override fun createCapture(aspectRatio: Int, rotation: Int): ImageCapture {

        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(FLASH_MODE_AUTO)
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
    }

    override fun onCaptureImageClick(button: ImageButton) {

        // Handle capture image
        handleCaptureImage()
    }

    private fun handleCaptureImage() {

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

        // Take picture
        capture?.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                println()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed", exception)
            }
        })

        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Display flash animation to indicate that photo was captured
            view?.postDelayed({
                view?.foreground = ColorDrawable(Color.WHITE)
                view?.postDelayed(
                    { view?.foreground = null }, ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    override fun onDisplayChanged(view: View) {
        capture?.targetRotation = view.display.rotation
    }

    override fun onGetPickResult(data: Intent?) {

        if (data != null) {
            data.data?.let { photoUri ->
                proceedNext(photoUri)
            }
        }
    }

    override fun pickFromGallery() {
        pickMediaFromGallery(REQUEST_PICK_LIBRARY, "image/*")
    }

    private fun proceedNext(photoUri: Uri) {
        val fragment = requireParentFragment().requireParentFragment()
        if (fragment is PostTypesFragment) {
            fragment.next(photoUri = photoUri)
        }
    }
}
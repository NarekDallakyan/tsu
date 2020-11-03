package social.tsu.android.ui.post.helper

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.GLException
import android.os.Environment
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.applovin.sdk.AppLovinSdkUtils.runOnUiThread
import social.tsu.android.R
import social.tsu.android.helper.DeviceUtils.Companion.getDeviceFullHeight
import social.tsu.android.helper.DeviceUtils.Companion.getDeviceWidth
import social.tsu.camerarecorder.CameraRecordListener
import social.tsu.camerarecorder.CameraRecorder
import social.tsu.camerarecorder.CameraRecorderBuilder
import social.tsu.camerarecorder.LensFacing
import social.tsu.camerarecorder.widget.Filters
import social.tsu.camerarecorder.widget.SampleGLView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10

class CameraHelper(
    private var activity: FragmentActivity,
    private var context: Context,
    private var rootView: View
) {

    private var isRecording: Boolean = false
    private var isCapturing: Boolean = false

    private var sampleGLView: SampleGLView? = null
    private var cameraRecorder: CameraRecorder? = null
    private var filepath: String? = null
    private var lensFacing: LensFacing = LensFacing.BACK
    private var cameraWidth = getDeviceWidth(activity)
    private var cameraHeight = getDeviceFullHeight(context)
    private var videoWidth = getDeviceWidth(activity)
    private var videoHeight = getDeviceFullHeight(context)
    private var toggleClick = false
    private var cameraView: ViewGroup? = null
    private var completeRecording: ((String?) -> Unit)? = null

    fun isRecording() = isRecording

    fun isCapturing() = isCapturing

    fun onResume() {
        initViews()
        setUpCamera()
        print("")
    }

    private fun initViews() {
        cameraView = rootView.findViewById(R.id.wrap_view)
    }

    fun onStop() {
        releaseCamera()
    }

    fun setUpCameraView() {

        activity.runOnUiThread {
            val frameLayout: FrameLayout = cameraView as FrameLayout
            frameLayout.removeAllViews()
            sampleGLView = null
            sampleGLView =
                SampleGLView(context.applicationContext)
            sampleGLView!!.setTouchListener(SampleGLView.TouchListener { event: MotionEvent, width: Int, height: Int ->
                if (cameraRecorder == null) return@TouchListener
                cameraRecorder!!.changeManualFocusPoint(event.x, event.y, width, height)
            })
            frameLayout.addView(sampleGLView)
        }
    }

    fun releaseCamera() {

        if (sampleGLView != null) {
            sampleGLView!!.onPause()
        }
        if (cameraRecorder != null) {
            cameraRecorder!!.stop()
            cameraRecorder!!.release()
            cameraRecorder = null
        }
        if (sampleGLView != null) {
            (cameraView as FrameLayout).removeView(sampleGLView)
            sampleGLView = null
        }
    }

    private fun setUpCamera() {
        setUpCameraView()
        cameraRecorder =
            CameraRecorderBuilder(activity, sampleGLView) //.recordNoFilter(true)
                .cameraRecordListener(object : CameraRecordListener {
                    override fun onGetFlashSupport(flashSupport: Boolean) {

                    }

                    override fun onRecordComplete() {
                        if (filepath != null) {
                            exportMp4ToGallery(context.applicationContext, filepath!!)
                        }
                    }

                    override fun onRecordStart() {}
                    override fun onError(exception: Exception) {

                    }

                    override fun onCameraThreadFinish() {
                        if (toggleClick) {
                            activity.runOnUiThread { setUpCamera() }
                        }
                        toggleClick = false
                    }
                })
                .videoSize(videoWidth.toInt(), videoHeight.toInt())
                .cameraSize(cameraWidth.toInt(), cameraHeight.toInt())
                .lensFacing(lensFacing)
                .build()
    }

    fun exportMp4ToGallery(context: Context, filePath: String) {
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, filePath)
        context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse(filePath)
            )
        )
        completeRecording?.let {
            it(filePath)
        }
    }

    fun changeFilter(filters: Filters) {
        if (cameraRecorder == null) {
            return
        }
        cameraRecorder!!.setFilter(
            Filters.getFilterInstance(
                filters,
                context.applicationContext
            )
        )
    }

    private interface BitmapReadyCallbacks {
        fun onBitmapReady(bitmap: Bitmap?)
    }

    private fun captureBitmap(bitmapReadyCallbacks: BitmapReadyCallbacks) {
        sampleGLView!!.queueEvent {
            val egl = EGLContext.getEGL() as EGL10
            val gl = egl.eglGetCurrentContext().gl as GL10
            val snapshotBitmap: Bitmap? = createBitmapFromGLSurface(
                sampleGLView!!.measuredWidth,
                sampleGLView!!.measuredHeight,
                gl
            )
            runOnUiThread { bitmapReadyCallbacks.onBitmapReady(snapshotBitmap) }
        }
    }

    private fun createBitmapFromGLSurface(w: Int, h: Int, gl: GL10): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer: IntBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            var texturePixel: Int
            var blue: Int
            var red: Int
            var pixel: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    texturePixel = bitmapBuffer[offset1 + j]
                    blue = texturePixel shr 16 and 0xff
                    red = texturePixel shl 16 and 0x00ff0000
                    pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            return null
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
    }

    fun saveAsPngImage(bitmap: Bitmap, filePath: String?) {
        try {
            val file = File(filePath)
            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getVideoFilePath(): String? {
        return getAndroidMoviesFolder().absolutePath + "/" + SimpleDateFormat("yyyyMM_dd-HHmmss").format(
            Date()
        ) + "cameraRecorder.mp4"
    }

    fun getAndroidMoviesFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    private fun exportPngToGallery(context: Context, filePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(filePath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
    }

    fun getImageFilePath(): String? {
        return getAndroidImageFolder().absolutePath + "/" + SimpleDateFormat("yyyyMM_dd-HHmmss").format(
            Date()
        ) + "cameraRecorder.png"
    }

    fun getAndroidImageFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }

    fun switchCamera() {

        releaseCamera()
        lensFacing = if (lensFacing == LensFacing.BACK) {
            LensFacing.FRONT
        } else {
            LensFacing.BACK
        }
        toggleClick = true
    }

    fun handleFlash() {

        if (!cameraRecorder!!.isFlashSupport) {
            Toast.makeText(context, "This device not support the flash mode", Toast.LENGTH_LONG)
                .show()
            return
        }

        if (cameraRecorder != null) {
            cameraRecorder!!.switchFlashMode()
            cameraRecorder!!.changeAutoFocus()
        }
    }

    fun startRecording(path: String) {
        this.filepath = path
        cameraRecorder?.start(filepath)
    }

    fun stopRecording(function: ((String?) -> Unit)? = null) {
        this.completeRecording = function
        cameraRecorder?.stop()
    }

    fun capturePicture(function: ((String) -> Unit)? = null) {

        isCapturing = true
        captureBitmap(object : BitmapReadyCallbacks {
            override fun onBitmapReady(bitmap: Bitmap?) {
                if (bitmap == null) {
                    Toast.makeText(
                        context,
                        "Capture image problem, please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                val imagePath = saveBitmap(bitmap)
                if (imagePath != null) {
                    function?.let {
                        activity.runOnUiThread {
                            it(imagePath)
                        }
                    }
                }
                isCapturing = false
            }
        })
    }

    fun saveBitmap(bitmap: Bitmap): String? {
        val imagePath = File(
            Environment.getExternalStorageDirectory()
                .toString() + "/Piyush.png"
        )
        val fos: FileOutputStream
        return try {
            fos = FileOutputStream(imagePath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            imagePath.path
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            null
        }
    }
}
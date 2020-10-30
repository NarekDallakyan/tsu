package social.tsu.android.ui.new_post

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.common.util.concurrent.ListenableFuture
import social.tsu.android.R
import social.tsu.android.ui.CameraUtil
import social.tsu.android.ui.MainActivity
import social.tsu.android.utils.snack
import java.io.File
import java.util.concurrent.Executor

/**
 * Base class for fragments that wants to use camera feature
 * Provides base functionality and base layout for capturing media: preview, shutter button, front/rear
 * camera switch and "pick from gallery" button
 *
 * Abstract methods needs to be overriden in order to provide capture functionality in resulting fragment:
 * - fun createCapture(aspectRatio: Int, rotation: Int): Capture
 * - fun onCaptureImageClick(button: ImageButton)
 * - fun onDisplayChanged(view: View)
 * - fun onGetPickResult(data: Intent?)
 * - fun pickFromGallery()
 *
 * @see UseCase
 */
abstract class BaseCameraFragment<Capture : UseCase> : Fragment() {

    companion object {
        const val REQUEST_PICK_LIBRARY = 100
    }

    protected val TAG = this::class.simpleName ?: "BaseCameraFragment"

    protected var capture: Capture? = null
    protected lateinit var cameraContainer: ConstraintLayout
    protected lateinit var cameraExecutor: Executor

    private lateinit var viewFinder: PreviewView
    private lateinit var displayManager: DisplayManager

    /**
     * Output directory for captured media
     */
    protected lateinit var outputDirectory: File

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var displayId = -1
    protected var lensFacing = CameraSelector.LENS_FACING_BACK

    protected lateinit var preview: Preview
    protected lateinit var cameraSwitchButton: ImageButton
    protected lateinit var cameraCaptureButton: ImageButton
    protected lateinit var photoPickButton: ImageButton

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            val view = requireActivity().window.decorView.rootView ?: return
            if (displayId == this@BaseCameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                onDisplayChanged(view)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        cameraExecutor = ContextCompat.getMainExecutor(requireActivity())

        cameraContainer = view.findViewById(R.id.camera_container)
        viewFinder = cameraContainer.findViewById(R.id.preview_view)

        displayManager = viewFinder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        outputDirectory = MainActivity.getOutputDirectory(requireActivity())

        cameraSwitchButton = view.findViewById(R.id.camera_switch_button)
        cameraCaptureButton = view.findViewById(R.id.camera_capture_button)
        photoPickButton = view.findViewById(R.id.photo_view_button)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listener for button used to capture photo
        cameraCaptureButton.setOnClickListener {
            try {
                onCaptureImageClick(cameraCaptureButton)
            } catch (e: Exception){
                Log.e(TAG, "onCaptureImageClick", e)
            }
        }

        // Listener for button used to view last photo
        photoPickButton.setOnClickListener {
            try {
                pickFromGallery()
            } catch (e: Exception){
                Log.e(TAG, "pickFromGallery", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onResume() {
        // Wait for the views to be properly laid out
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display?.displayId ?: - 1
            resetCamera()
        }
        super.onResume()
    }

    @SuppressLint("RestrictedApi")
    override fun onPause() {
        if (CameraX.isInitialized()) {
            CameraX.unbindAll()
        }
        super.onPause()
    }

    @SuppressLint("RestrictedApi")
    override fun onDetach() {
        super.onDetach()
        // showBars()
        // CameraX.shutdown()
    }

    private fun setUpPinchToZoom(camera: androidx.camera.core.Camera) {

        val cameraControl = camera.cameraControl
        val cameraInfo = camera.cameraInfo
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = cameraInfo.zoomState.value?.zoomRatio ?: 0F
                val delta = detector.scaleFactor
                cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, listener)

        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    @SuppressLint("RestrictedApi")
    private fun resetCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                setupCameraSwitchButton(cameraProvider)
                bindCameraUseCases(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Can't reset camera", e)
                snack("Can't run camera")
            }
        }, cameraExecutor)
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val metrics = DisplayMetrics().also { viewFinder.display?.getRealMetrics(it) }

        val screenAspectRatio = CameraUtil.aspectRatio(
            metrics.widthPixels,
            metrics.heightPixels
        )

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val rotation = viewFinder.display?.rotation ?: Surface.ROTATION_0

        preview = Preview.Builder()
            .setTargetName("Preview")
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        capture = createCapture(screenAspectRatio, rotation)

        cameraProvider.unbindAll()

        try {
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                capture,
                preview
            )
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider())
            setUpPinchToZoom(camera)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun setupCameraSwitchButton(cameraProvider: ProcessCameraProvider) {
        // Listener for button used to switch cameras
        cameraSwitchButton.setOnClickListener {
            if (!canSwitchCamera()) return@setOnClickListener

            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

            try {
                cameraProvider.unbindAll()
                bindCameraUseCases(cameraProvider)
            } catch (exc: CameraInfoUnavailableException) {
                // Do nothing
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PICK_LIBRARY -> onGetPickResult(data)
            else -> return
        }
    }

    /**
     * Provides {@link Capture} instance to use with camera
     */
    abstract fun createCapture(aspectRatio: Int, rotation: Int): Capture

    /**
     * Called when user taps capture button. You may want to handle this event differently (i.e. when
     * recording video: change capture button appearance to represent recording state)
     */
    abstract fun onCaptureImageClick(button: ImageButton)


    abstract fun onDisplayChanged(view: View)

    /**
     * Called after user picked existing media and you need to parse result from picker intent
     */
    abstract fun onGetPickResult(data: Intent?)

    /**
     * Called when user taps on "Pick from gallery" button. Must create correct picker intent and launch
     * media picker. Result will arrive in {@link #onGetPickResult(Intent?)
     */
    abstract fun pickFromGallery()

    /**
     * Override if you need to disable or control switching cameras
     */
    protected open fun canSwitchCamera(): Boolean = true

}

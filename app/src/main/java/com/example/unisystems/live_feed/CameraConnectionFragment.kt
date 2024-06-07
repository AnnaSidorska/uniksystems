package com.example.unisystems.live_feed

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.unisystems.R
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


@SuppressLint("ValidFragment")
class CameraConnectionFragment @SuppressLint("ValidFragment") private constructor(
    private val cameraConnectionCallback: ConnectionCallback,
    private val imageListener: OnImageAvailableListener,
    private val layout: Int,
    private val inputSize: Size
) : Fragment() {
    companion object {
        private const val MINIMUM_PREVIEW_SIZE = 320
        private val ORIENTATIONS = SparseIntArray()
        private const val FRAGMENT_DIALOG = "dialog"

        private fun chooseOptimalSize(
            choices: Array<Size>,
            width: Int,
            height: Int
        ): Size {
            val minSize = max(
                min(width, height),
                MINIMUM_PREVIEW_SIZE
            )
            val desiredSize = Size(width, height)

            var exactSizeFound = false
            val bigEnough: MutableList<Size> =
                ArrayList()
            val tooSmall: MutableList<Size> =
                ArrayList()
            for (option in choices) {
                if (option == desiredSize) {
                    exactSizeFound = true
                }
                if (option.height >= minSize && option.width >= minSize) {
                    bigEnough.add(option)
                } else {
                    tooSmall.add(option)
                }
            }
            if (exactSizeFound) {
                return desiredSize
            }

            return if (bigEnough.size > 0) {
                Collections.min(
                    bigEnough,
                    CompareSizesByArea()
                )
            } else {
                choices[0]
            }
        }

        fun newInstance(
            callback: ConnectionCallback,
            imageListener: OnImageAvailableListener,
            layout: Int,
            inputSize: Size
        ): CameraConnectionFragment {
            return CameraConnectionFragment(callback, imageListener, layout, inputSize)
        }

        init {
            ORIENTATIONS.append(
                Surface.ROTATION_0,
                90
            )
            ORIENTATIONS.append(
                Surface.ROTATION_90,
                0
            )
            ORIENTATIONS.append(
                Surface.ROTATION_180,
                270
            )
            ORIENTATIONS.append(
                Surface.ROTATION_270,
                180
            )
        }
    }

    private val cameraOpenCloseLock =
        Semaphore(1)

    private val captureCallback: CaptureCallback = object : CaptureCallback() {
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
        }
    }
    private var cameraId: String? = null
    private var textureView: AutoFitTextureView? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var sensorOrientation: Int? = null
    private var previewSize: Size? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val surfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            texture: SurfaceTexture, width: Int, height: Int
        ) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(
            texture: SurfaceTexture, width: Int, height: Int
        ) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
    }
    private var previewReader: ImageReader? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewRequest: CaptureRequest? = null
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(cd: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release()
            cameraDevice = cd
            createCameraPreviewSession()
        }

        override fun onDisconnected(cd: CameraDevice) {
            cameraOpenCloseLock.release()
            cd.close()
            cameraDevice = null
        }

        override fun onError(cd: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cd.close()
            cameraDevice = null
            val activity = activity
            activity?.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        textureView = view.findViewById<View>(R.id.texture) as AutoFitTextureView
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            openCamera(textureView!!.width, textureView!!.height)
        } else {
            textureView!!.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    fun setCamera(cameraId: String?) {
        this.cameraId = cameraId
    }

    private fun setUpCameraOutputs() {
        val activity = this.activity
        val manager =
            activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)


            previewSize = chooseOptimalSize(
                map!!.getOutputSizes(SurfaceTexture::class.java),
                inputSize.width,
                inputSize.height
            )

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView!!.setAspectRatio(previewSize!!.width, previewSize!!.height)
            } else {
                textureView!!.setAspectRatio(previewSize!!.height, previewSize!!.width)
            }
        } catch (_: CameraAccessException) {

        } catch (e: NullPointerException) {

            ErrorDialog.newInstance("getString(R.string.tfe_ic_camera_error)")
                .show(childFragmentManager, FRAGMENT_DIALOG)
            throw IllegalStateException("getString(R.string.tfe_ic_camera_error)")
        }
        cameraConnectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation!!)
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs()
        configureTransform(width, height)
        val activity = activity
        val manager =
            activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        } catch (_: CameraAccessException) {

        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession!!.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (null != previewReader) {
                previewReader!!.close()
                previewReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageListener")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // LOGGER.e(e, "Exception!");
        }
    }

    @Suppress("DEPRECATION")
    private fun createCameraPreviewSession() {
        try {
            if (textureView != null) {
                val texture = textureView!!.surfaceTexture!!
                texture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
                val surface = Surface(texture)

                previewRequestBuilder =
                    cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder!!.addTarget(surface)

                previewReader = ImageReader.newInstance(
                    previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
                )
                previewReader!!.setOnImageAvailableListener(imageListener, backgroundHandler)
                previewRequestBuilder!!.addTarget(previewReader!!.surface)

                cameraDevice!!.createCaptureSession(
                    listOf(surface, previewReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (null == cameraDevice) {
                                return
                            }
                            captureSession = cameraCaptureSession
                            try {
                                previewRequestBuilder!!.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                previewRequestBuilder!!.set(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                                )
                                previewRequest = previewRequestBuilder!!.build()
                                captureSession!!.setRepeatingRequest(
                                    previewRequest!!, captureCallback, backgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                // LOGGER.e(e, "Exception!");
                            }
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            val activity = activity
                            activity?.runOnUiThread { Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show() }
                        }
                    },
                    null
                )
            }
        } catch (e: CameraAccessException) {
            // LOGGER.e(e, "Exception!");
        }
    }

    @Suppress("DEPRECATION")
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity
        if (null == textureView || null == previewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0F, 0F,
            previewSize!!.height.toFloat(),
            previewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90 * (rotation - 2).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView!!.setTransform(matrix)
    }

    interface ConnectionCallback {
        val screenOrientation: Any

        fun onPreviewSizeChosen(size: Size?, cameraRotation: Int)
    }

    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            return java.lang.Long.signum(
                lhs!!.width.toLong() * lhs.height - rhs!!.width.toLong() * rhs.height
            )
        }
    }

    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            val message = arguments?.getString(ARG_MESSAGE)
            return AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(
                    android.R.string.ok
                ) { _, _ -> activity?.finish() }
                .create()
        }

        companion object {
            private const val ARG_MESSAGE = "message"
            fun newInstance(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }


}
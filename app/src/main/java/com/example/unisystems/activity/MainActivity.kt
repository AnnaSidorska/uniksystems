package com.example.unisystems.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.unisystems.R
import com.example.unisystems.drawing.BorderedText
import com.example.unisystems.drawing.MultiBoxTracker
import com.example.unisystems.drawing.OverlayView
import com.example.unisystems.face_recognition.FaceClassifier
import com.example.unisystems.face_recognition.TFLiteFaceRecognition
import com.example.unisystems.live_feed.CameraConnectionFragment
import com.example.unisystems.live_feed.ImageUtils.convertYUV420ToARGB8888
import com.example.unisystems.live_feed.ImageUtils.getTransformationMatrix
import com.example.unisystems.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException

open class MainActivity : AppCompatActivity(), OnImageAvailableListener {
    private lateinit var handler: Handler
    private lateinit var frameToCropTransform: Matrix
    private var sensorOrientation = 0
    private lateinit var cropToFrameTransform: Matrix
    var trackingOverlay: OverlayView? = null
    private lateinit var borderedText: BorderedText
    private lateinit var tracker: MultiBoxTracker
    private var useFacing: Int? = null

    private lateinit var detector: FaceDetector

    private lateinit var faceClassifier: FaceClassifier
    private var registerFace = false

    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private lateinit var postInferenceCallback: Runnable
    private lateinit var imageConverter: Runnable
    private lateinit var rgbFrameBitmap: Bitmap

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var userRef: DatabaseReference

    var previewHeight = 0
    var previewWidth = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler = Handler(Looper.myLooper()!!)


        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_DENIED
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permission, 121)
        }
        val intent = intent
        useFacing = intent.getIntExtra(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_BACK)

        setFragment()

        tracker = MultiBoxTracker(this)

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)


        try {
            faceClassifier = TFLiteFaceRecognition.create(
                assets,
                "facenet.tflite",
                TF_OD_API_INPUT_SIZE2,
                false, applicationContext
            )
        } catch (e: IOException) {
            e.printStackTrace()
            val toast = Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
        findViewById<View>(R.id.imageView4).setOnClickListener { registerFace = true }
        findViewById<View>(R.id.imageView3).setOnClickListener { switchCamera() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 121 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setFragment()
        }
    }

    private fun setFragment() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null
        try {
            cameraId = manager.cameraIdList[(useFacing)!!]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        val fragment: Fragment
        val camera2Fragment = CameraConnectionFragment.newInstance(
            object : CameraConnectionFragment.ConnectionCallback {
                override val screenOrientation: Int
                    get() {
                        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val display = display
                            display?.rotation
                        } else {
                            @Suppress("DEPRECATION")
                            windowManager.defaultDisplay.rotation
                        }

                        return when (rotation) {
                            Surface.ROTATION_270 -> 270
                            Surface.ROTATION_180 -> 180
                            Surface.ROTATION_90 -> 90
                            else -> 0
                        }
                    }


                override fun onPreviewSizeChosen(size: Size?, cameraRotation: Int) {
                    previewHeight = size!!.height
                    previewWidth = size.width
                    val textSizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
                    )
                    borderedText = BorderedText(textSizePx)
                    borderedText.setTypeface(Typeface.MONOSPACE)
                    val cropSize = CROP_SIZE
                    previewWidth = size.width
                    previewHeight = size.height
                    sensorOrientation = cameraRotation - screenOrientation
                    rgbFrameBitmap =
                        Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
                    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
                    frameToCropTransform = getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT
                    )
                    cropToFrameTransform = Matrix()
                    frameToCropTransform.invert(cropToFrameTransform)
                    trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
                    trackingOverlay!!.addCallback(
                        object : OverlayView.DrawCallback {
                            override fun drawCallback(canvas: Canvas?) {
                                if (canvas != null) {
                                    tracker.draw(canvas)
                                }
                                // Log.d("tryDrawRect", "inside draw")
                            }
                        })
                    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation)
                }
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )
        camera2Fragment.setCamera(cameraId)
        fragment = camera2Fragment
        val fragmentManager = supportFragmentManager

        fragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onImageAvailable(reader: ImageReader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true

            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                convertYUV420ToARGB8888(
                    (yuvBytes[0])!!,
                    (yuvBytes[1])!!,
                    (yuvBytes[2])!!,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }
            performFaceDetection()
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing image: ${e.message}")
        }
    }


    private fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]!!]
        }
    }

    private lateinit var croppedBitmap: Bitmap
    private lateinit var mappedRecognitions: ArrayList<FaceClassifier.Recognition>

    private fun performFaceDetection() {
        imageConverter.run()
        rgbFrameBitmap.setPixels(rgbBytes!!, 0, previewWidth, 0, 0, previewWidth, previewHeight)
        val canvas = Canvas(croppedBitmap)
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)
        Handler(Looper.myLooper()!!).post {
            mappedRecognitions = ArrayList()
            val image = InputImage.fromBitmap((croppedBitmap), 0)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    for (face: Face in faces) {
                        performFaceRecognition(face)
                    }
                    registerFace = false
                    tracker.trackResults(mappedRecognitions) //,10
                    trackingOverlay!!.postInvalidate()
                    postInferenceCallback.run()
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to detect faces")
                    Toast.makeText(this, "Не вдалося виявити обличчя.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun performFaceRecognition(face: Face) {
        val bounds: Rect = face.boundingBox
        if (bounds.top < 0) {
            bounds.top = 0
        }
        if (bounds.left < 0) {
            bounds.left = 0
        }
        if (bounds.left + bounds.width() > croppedBitmap.width) {
            bounds.right = croppedBitmap.width - 1
        }
        if (bounds.top + bounds.height() > croppedBitmap.height) {
            bounds.bottom = croppedBitmap.height - 1
        }
        var crop: Bitmap = Bitmap.createBitmap(
            croppedBitmap,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height() - 30
        )
        crop = Bitmap.createScaledBitmap(crop, TF_OD_API_INPUT_SIZE2, TF_OD_API_INPUT_SIZE2, false)
        val result: FaceClassifier.Recognition? = faceClassifier.recognizeImage(crop, registerFace)
        var title = "Незареєстрований користувач"
        var confidence = 0f
        if (result != null) {
            if (registerFace) {
                registerFaceDialogue(crop, result)
            } else {
                if (result.distance!! < 0.75f) {
                    confidence = result.distance
                    title = result.title.toString()

                }
            }
        }
        val location = RectF(bounds)
        if (useFacing == CameraCharacteristics.LENS_FACING_BACK) {
            location.right = croppedBitmap.width - location.right
            location.left = croppedBitmap.width - location.left
        }
        cropToFrameTransform.mapRect(location)
        val recognition =
            FaceClassifier.Recognition(
                face.trackingId.toString() + "",
                title,
                confidence,
                location
            )
        mappedRecognitions.add(recognition)
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.close()
    }

    private fun registerFaceDialogue(croppedFace: Bitmap, rec: FaceClassifier.Recognition?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.register_face_dialogue)
        val ivFace = dialog.findViewById<ImageView>(R.id.dlg_image)
        val nameEd = dialog.findViewById<TextView>(R.id.registerFaceName)
        val surnameEd = dialog.findViewById<TextView>(R.id.registerFaceSurname)
        val register = dialog.findViewById<Button>(R.id.button2)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        firebaseDatabase = FirebaseDatabase.getInstance()
        val userId = auth.currentUser!!.uid
        val database = Firebase.database(DATABASE_URL)
        userRef = database.getReference("users/$userId")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        nameEd.text = user.name
                        surnameEd.text = user.surname
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error fetching user data: ${error.message}")
            }
        })

        ivFace.setImageBitmap(croppedFace)
        register.setOnClickListener {
            val name = nameEd.text.toString().trim()
            val surname = surnameEd.text.toString().trim()
            val fullName = "$name $surname"

            faceClassifier.register(userId, fullName, rec)
            Toast.makeText(this@MainActivity, "Ваше обличчя зареєстроване", Toast.LENGTH_SHORT)
                .show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun switchCamera() {
        val intent = intent
        useFacing = if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            CameraCharacteristics.LENS_FACING_BACK
        } else {
            CameraCharacteristics.LENS_FACING_FRONT
        }
        intent.putExtra(KEY_USE_FACING, useFacing)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        restartWith(intent)
    }

    private fun restartWith(intent: Intent) {
        finish()
        startActivity(intent)
    }

    companion object {
        const val TAG = "Main Activity"
        private const val MAINTAIN_ASPECT = false
        private const val TEXT_SIZE_DIP = 10f
        private const val KEY_USE_FACING = "use_facing"
        private const val CROP_SIZE = 1000
        private const val TF_OD_API_INPUT_SIZE2 = 160
        private const val DATABASE_URL: String = "https://uni-k-systems-default-rtdb.europe-west1.firebasedatabase.app"
    }
}
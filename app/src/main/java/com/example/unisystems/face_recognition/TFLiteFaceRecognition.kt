package com.example.unisystems.face_recognition

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.util.Pair
import com.example.unisystems.database.FaceRecognitionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.sqrt

class TFLiteFaceRecognition private constructor(ctx: Context) : FaceClassifier {
    private var isModelQuantized = false

    private var inputSize = 0
    private lateinit var intValues: IntArray
    private lateinit var embeddings: Array<FloatArray>
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null
    private var registered = HashMap<String?, FaceClassifier.Recognition>()

    private var db: FaceRecognitionHelper = FaceRecognitionHelper()

    init {
        CoroutineScope(Dispatchers.Main).launch {
            registered = db.getAllFaces()
            Log.d("Faces", "Fetched faces: $registered")
        }
    }

    private fun findNearest(emb: FloatArray): Pair<String?, Float> {
        var ret: Pair<String?, Float>? = null
        for ((name, value) in registered) {
            val knownEmb = (value.embedding as Array<FloatArray>?)!![0]
            var distance = 0f
            for (i in emb.indices) {
                val diff = emb[i] - knownEmb[i]
                distance += diff * diff
            }
            distance = sqrt(distance.toDouble()).toFloat()
            if (ret == null || distance < ret.second) {
                ret = Pair(name, distance)
            }
        }
        Log.d("tryRes","Below= "+ ret!!.first+"   "+ ret.second)
        return ret
    }

    override fun register(uid: String, name: String?, recognition: FaceClassifier.Recognition?) {
        db.insertFace(uid, name, recognition!!)
        registered[name] = recognition
    }

    override fun registerVisit(uid: String, croppedFace: Bitmap, name: String, surname: String, building: String,
        recognition: FaceClassifier.Recognition) {
        CoroutineScope(Dispatchers.Main).launch {
            db.insertVisit(uid, croppedFace, name, surname, building, recognition)
        }
    }

    override fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): FaceClassifier.Recognition {
        val byteBuffer = ByteBuffer.allocateDirect(4 * bitmap!!.width * bitmap.width * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(bitmap.width * bitmap.width)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.width) {
                val input = intValues[pixel++]
                byteBuffer.putFloat((((input.shr(16)  and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
            }
        }

        val inputArray = arrayOf<Any?>(byteBuffer)
        val outputMap: MutableMap<Int, Any> = HashMap()
        embeddings = Array(1) { FloatArray(OUTPUT_SIZE) }
        outputMap[0] = embeddings
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        Log.d("tryResr",embeddings[0].contentToString())
        var distance = Float.MAX_VALUE
        val id = "0"
        var label: String? = "?"
        if (registered.size > 0) {
            val nearest = findNearest(embeddings[0])
            val name = nearest.first
            label = name
            distance = nearest.second
        }

        val rec = FaceClassifier.Recognition(
            id,
            label,
            distance,
            RectF()
        )
        if (getExtra) {
            rec.embedding = embeddings
        }
        return rec
    }

    companion object {
        private const val OUTPUT_SIZE = 512
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
            val fileDescriptor = assets.openFd(modelFilename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            inputSize: Int,
            isQuantized: Boolean, ctx: Context
        ): FaceClassifier {
            val d = TFLiteFaceRecognition(ctx)
            d.inputSize = inputSize
            try {
                d.tfLite = Interpreter(loadModelFile(assetManager, modelFilename))
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
            d.isModelQuantized = isQuantized
            val numBytesPerChannel: Int = if (isQuantized) {
                1
            } else {
                4
            }
            d.imgData =
                ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel)
            d.intValues = IntArray(d.inputSize * d.inputSize)
            return d
        }
    }
}
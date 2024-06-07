package com.example.unisystems.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Join
import android.graphics.RectF
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.util.TypedValue
import com.example.unisystems.face_recognition.FaceClassifier
import com.example.unisystems.live_feed.ImageUtils

import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import kotlin.math.min


class MultiBoxTracker(context: Context) {
    private val screenRects: MutableList<Pair<Float, RectF>> = LinkedList()

    private val availableColors: Queue<Int> = LinkedList()
    private val trackedObjects: MutableList<TrackedRecognition> = LinkedList()
    private val boxPaint = Paint()
    private val textSizePx: Float
    private val borderedText: BorderedText
    private var frameToCanvasMatrix: Matrix? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0

    init {
        for (color in COLORS) {
            availableColors.add(color)
        }
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 5.0f
        boxPaint.strokeCap = Cap.ROUND
        boxPaint.strokeJoin = Join.ROUND
        boxPaint.strokeMiter = 100f
        textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
    }

    @Synchronized
    fun setFrameConfiguration(
        width: Int, height: Int, sensorOrientation: Int
    ) {
        frameWidth = width
        frameHeight = height
        this.sensorOrientation = sensorOrientation
    }


    @Synchronized
    fun trackResults(results: List<FaceClassifier.Recognition>, ) {//timestamp: Long
        processResults(results)
    }

    @Synchronized
    fun draw(canvas: Canvas) {
        Log.d("tryDrawRect", "inside draw 2")
        val rotated = sensorOrientation % 180 == 90
        val multiplier = min(
            canvas.height / (if (rotated) frameWidth else frameHeight).toFloat(),
            canvas.width / (if (rotated) frameHeight else frameWidth).toFloat()
        )
        frameToCanvasMatrix = ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (multiplier * if (rotated) frameHeight else frameWidth).toInt(),
            (multiplier * if (rotated) frameWidth else frameHeight).toInt(),
            sensorOrientation,
            false
        )
        Log.d("tryDrawRect", "inside draw " + trackedObjects.size)
        for (recognition in trackedObjects) {
            val trackedPos = RectF(recognition.location)
            frameToCanvasMatrix!!.mapRect(trackedPos)
            boxPaint.color = recognition.color
            val cornerSize = min(trackedPos.width(), trackedPos.height()) / 8.0f
            canvas.drawRect(trackedPos, boxPaint)
            val labelString = if (!TextUtils.isEmpty(recognition.title)) String.format(
                Locale.US,
                "%s %.2f",
                recognition.title,
                recognition.detectionConfidence
            ) else String.format(Locale.US, "%.2f", recognition.detectionConfidence)

            //Log.d("tryDrawRect", labelString)
            borderedText.drawText(
                canvas, trackedPos.left + cornerSize, trackedPos.top, labelString, boxPaint
            )
        }
    }

    private fun processResults(results: List<FaceClassifier.Recognition>) {
        val rectsToTrack: MutableList<Pair<Float, FaceClassifier.Recognition>> =
            LinkedList<Pair<Float, FaceClassifier.Recognition>>()
        screenRects.clear()
        val rgbFrameToScreen = Matrix(frameToCanvasMatrix)
        for (result in results) {
            val detectionFrameRect = RectF(result.getLocation())
            val detectionScreenRect = RectF()
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect)

            screenRects.add(Pair(result.distance, detectionScreenRect))
            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                //logger.w("Degenerate rectangle! " + detectionFrameRect);
                continue
            }
            rectsToTrack.add(Pair<Float, FaceClassifier.Recognition>(result.distance, result))
        }
        trackedObjects.clear()
        if (rectsToTrack.isEmpty()) {
            return
        }
        for (potential in rectsToTrack) {
            val trackedRecognition = TrackedRecognition()
            trackedRecognition.detectionConfidence = potential.first
            trackedRecognition.location = RectF(potential.second.getLocation())
            trackedRecognition.title = potential.second.title
            trackedRecognition.color = COLORS[trackedObjects.size]
            trackedObjects.add(trackedRecognition)
            if (trackedObjects.size >= COLORS.size) {
                break
            }
        }
    }

    private class TrackedRecognition {
        var location: RectF? = null
        var detectionConfidence = 0f
        var color = 0
        var title: String? = null
    }

    companion object {
        private const val TEXT_SIZE_DIP = 18f
        private const val MIN_SIZE = 16.0f
        private val COLORS = intArrayOf(
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
        )
    }
}

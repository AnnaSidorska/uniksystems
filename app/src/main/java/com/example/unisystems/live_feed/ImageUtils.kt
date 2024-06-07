package com.example.unisystems.live_feed

import android.graphics.Matrix
import kotlin.math.abs
import kotlin.math.max

object ImageUtils {
    private const val KMAXCHANNELVALUE = 262143

    private fun YUV2RGB(y1: Int, u1: Int, v1: Int): Int {
        var y = y1
        var u = u1
        var v = v1
        y = if (y - 16 < 0) 0 else y - 16
        u -= 128
        v -= 128

        val y1192 = 1192 * y
        var r = y1192 + 1634 * v
        var g = y1192 - 833 * v - 400 * u
        var b = y1192 + 2066 * u

        r = if (r > KMAXCHANNELVALUE) KMAXCHANNELVALUE else if (r < 0) 0 else r
        g = if (g > KMAXCHANNELVALUE) KMAXCHANNELVALUE else if (g < 0) 0 else g
        b = if (b > KMAXCHANNELVALUE) KMAXCHANNELVALUE else if (b < 0) 0 else b
        return -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
    }

    fun convertYUV420ToARGB8888(
            yData: ByteArray,
            uData: ByteArray,
            vData: ByteArray,
            width: Int,
            height: Int,
            yRowStride: Int,
            uvRowStride: Int,
            uvPixelStride: Int,
            out: IntArray) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)
            for (i in 0 until width) {
                val uvOffset = pUV + (i shr 1) * uvPixelStride
                out[yp++] = YUV2RGB(0xff and yData[pY + i].toInt(), 0xff and uData[uvOffset].toInt(), 0xff and vData[uvOffset].toInt())
            }
        }
    }

    fun getTransformationMatrix(
            srcWidth: Int,
            srcHeight: Int,
            dstWidth: Int,
            dstHeight: Int,
            applyRotation: Int,
            maintainAspectRatio: Boolean): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
//            if (applyRotation % 90 != 0) {
//
//            }

            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            matrix.postRotate(applyRotation.toFloat())
        }

        val transpose = (abs(applyRotation) + 90) % 180 == 0
        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()
            if (maintainAspectRatio) {

                val scaleFactor = max(scaleFactorX, scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {

                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }
}


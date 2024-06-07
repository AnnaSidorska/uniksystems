package com.example.unisystems.face_recognition

import android.graphics.Bitmap
import android.graphics.RectF
import java.util.Locale

interface FaceClassifier {
    fun register(uid: String, name: String?, recognition: Recognition?)
    fun registerVisit(uid: String, croppedFace: Bitmap, name: String, surname: String, building: String, recognition: Recognition)
    fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): Recognition?
    class Recognition {
        val id: String?
        val title: String?
        val distance: Float?
        var embedding: Any?
        private var location: RectF?
        private var crop: Bitmap?

        constructor(
            id: String?, title: String?, distance: Float?, location: RectF?
        ) {
            this.id = id
            this.title = title
            this.distance = distance
            this.location = location
            embedding = null
            crop = null
        }

        constructor(
            title: String?, embedding: Any?
        ) {
            id = null
            this.title = title
            distance = null
            location = null
            this.embedding = embedding
            crop = null
        }

        fun getLocation(): RectF {
            return RectF(location)
        }

        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (distance != null) {
                resultString += String.format(Locale.getDefault(), "(%.1f%%) ", distance * 100.0f)

            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            return resultString.trim { it <= ' ' }
        }
    }
}

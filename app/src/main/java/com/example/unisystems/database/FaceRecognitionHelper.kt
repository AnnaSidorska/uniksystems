package com.example.unisystems.database

import android.graphics.Bitmap
import android.util.Log
import com.example.unisystems.face_recognition.FaceClassifier
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class FaceRecognitionHelper {

    fun insertFace(uid: String, name: String?, recognition: FaceClassifier.Recognition): Boolean {
        if (recognition.embedding !is Array<*>) {
            Log.e(TAG, "Embedding is not of the expected type")
            return false
        }

        val floatList = recognition.embedding as Array<FloatArray>
        val embeddingList = floatList[0].toList()

        val db = Firebase.firestore
        val faceData = hashMapOf(
            "uid" to uid,
            "name" to name,
            "embedding" to embeddingList
        )

        db.collection("faces")
            .add(faceData)
            .addOnSuccessListener {
                Log.d(TAG, "Face data added successfully with ID: ${it.id}")
                return@addOnSuccessListener
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding face data", e)
                return@addOnFailureListener
            }
        return true
    }

    suspend fun insertVisit(uid: String, croppedFace: Bitmap, name: String?, surname: String?, building: String, recognition: FaceClassifier.Recognition): Boolean {
        val storage: FirebaseStorage = FirebaseStorage.getInstance()
        val imageId = UUID.randomUUID().toString()
        val imageRef = storage.reference.child("faceRegister/$imageId.jpg")

        val baos = ByteArrayOutputStream()
        croppedFace.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        return try {
            imageRef.putBytes(data).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            val db = Firebase.firestore
            val currentTime = System.currentTimeMillis()
            val visitData = hashMapOf(
                "userID" to uid,
                "name" to name,
                "surname" to surname,
                "distance" to recognition.distance,
                "imageUri" to downloadUrl,
                "registrationTime" to currentTime,
                "building" to building
            )

            db.collection("registerFaces").document(imageId)
                .set(visitData)
                .addOnSuccessListener {
                    Log.d(TAG, "Visit registration successful")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error registering visit", e)
                }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            false
        }
    }

    suspend fun getAllFaces(): HashMap<String?, FaceClassifier.Recognition> = withContext(
        Dispatchers.IO) {
        val registered = HashMap<String?, FaceClassifier.Recognition>()
        try {
            val db = Firebase.firestore
            val snapshot = db.collection("faces").get().await()

            for (document in snapshot.documents) {
                val name = document.getString("name")
                val embeddingList = document.get("embedding") as? List<Double>
                if (name != null && embeddingList != null) {
                    val floatArray = embeddingList.map { it.toFloat() }.toFloatArray()
                    val bigArray = arrayOf(floatArray)
                    val recognition = FaceClassifier.Recognition(name, bigArray)
                    registered[name] = recognition
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting documents: ", e)
        }

        Log.d("tryRL", "rl=${registered.size}")
        return@withContext registered
    }


    companion object {
        const val TAG = "FaceRecognitionHelper"
    }
}
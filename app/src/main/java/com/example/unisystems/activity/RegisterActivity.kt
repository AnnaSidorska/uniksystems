package com.example.unisystems.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.unisystems.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerBtn: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var loginLink: TextView

    private lateinit var progressBar: ProgressBar

    private var imageUri: Uri? = null

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null) {
            updateUI()
        }
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            var input: Bitmap? = uriToBitmap(imageUri!!)
            if (input != null) {
                input = rotateBitmap(input)
                imageView.setImageBitmap(input)
            } else {
                Toast.makeText(this, "Помилка при завантаженні зображення", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById(R.id.userImage)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        registerBtn = findViewById(R.id.processButton)

        auth = FirebaseAuth.getInstance()

        loginLink = findViewById(R.id.loginLink)

        progressBar = findViewById(R.id.reg_progress_bar)
        progressBar.visibility = View.INVISIBLE

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        imageView.setOnClickListener {
            val galleryIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            galleryActivityResultLauncher.launch(galleryIntent)
        }

        registerBtn.setOnClickListener {
            registerBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE

            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()
            val confirmPassText = confirmPassword.text.toString().trim()

            if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passText) || TextUtils.isEmpty(confirmPassText)) {
                email.error = "Заповніть, будь ласка, всі поля"
                password.error = "Заповніть, будь ласка, всі поля"
                confirmPassword.error = "Заповніть, будь ласка, всі поля"
                registerBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            } else if (passText != confirmPassText) {
                confirmPassword.error = "Паролі не співпадають"
                registerBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            } else if (passText.length < 6 || passText.length > 18) {
                password.error = "Пароль повинен містити від 6 до 18 символів"
                registerBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            } else {
                createUserAccount(emailText, passText)
            }
        }
    }

    private fun createUserAccount(userEmail: String, pass: String) {
        if (imageUri == null) {
            email.error = "Оберіть, будь ласка, фото"
            Toast.makeText(this, "Виберіть фото профілю", Toast.LENGTH_SHORT).show()
            registerBtn.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
            return
        }

        auth.createUserWithEmailAndPassword(userEmail, pass)
            .addOnCompleteListener(this) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Обліковий запис $userEmail створено.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val currentUser = auth.currentUser
                    registerBtn.visibility = View.INVISIBLE
                    progressBar.visibility = View.VISIBLE

                    updateUserInformation(userEmail, imageUri, currentUser!!)
                } else {
                    Toast.makeText(this, "Помилка! Обліковий запис не було створено.", Toast.LENGTH_SHORT).show()
                    registerBtn.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE
                }
            }
    }


    private fun updateUI() {
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun updateUserInformation(userEmail: String, pickedImageUri: Uri?, currentUser: FirebaseUser) {
        val mStorage: StorageReference = FirebaseStorage.getInstance().reference.child("users_photos")
        if (pickedImageUri != null) {
            val imageFilePath: StorageReference = mStorage.child(pickedImageUri.lastPathSegment!!)

            imageFilePath.putFile(pickedImageUri)
                .addOnSuccessListener { _ ->
                    imageFilePath.downloadUrl
                        .addOnSuccessListener { uri ->
                            val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(userEmail).setPhotoUri(uri).build()

                            currentUser.updateProfile(profileUpdate)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(this, "Реєстрація завершена.", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, CreateAccount::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Помилка при створенні користувача: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Завантаження припинене: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Неможливо створити новий акаунт без фото. Додайте фото!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        return try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor = parcelFileDescriptor?.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor?.close()
            image
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("Range")
    fun rotateBitmap(input: Bitmap): Bitmap {
        val orientationColumn = arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur = contentResolver.query(imageUri!!, orientationColumn, null, null, null)
        var orientation = -1
        cur?.use {
            if (it.moveToFirst()) {
                orientation = it.getInt(it.getColumnIndex(orientationColumn[0]))
            }
        }
        Log.d("tryOrientation", "$orientation")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    companion object {
        const val TAG = "RegisterActivity"
    }

}
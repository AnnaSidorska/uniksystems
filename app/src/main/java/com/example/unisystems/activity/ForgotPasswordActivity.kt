package com.example.unisystems.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.unisystems.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var sendBtn: Button
    private lateinit var email: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPassword)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sendBtn = findViewById(R.id.sendBtn)
        email = findViewById(R.id.sendEmail)
        progressBar = findViewById(R.id.reset_progress_bar)
        progressBar.visibility = View.INVISIBLE

        sendBtn.setOnClickListener {
            sendEmail()
        }
    }

    private fun sendEmail() {
        sendBtn.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE

        val userEmail = email.text.toString().trim()

        if(userEmail.isEmpty()) {
            email.error = "Введіть email!"
            sendBtn.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
            email.requestFocus()
        } else {
            if(userEmail.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(userEmail)
                    .addOnSuccessListener {
                        sendBtn.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Лист надіслано на пошту $userEmail", Toast.LENGTH_SHORT).show()
                        updateUI()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Сталася помилка. Лист не було надіслано!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                email.error = "Введіть email!"
                sendBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateUI() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

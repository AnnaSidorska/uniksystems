package com.example.unisystems.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.unisystems.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var forgotPassword: TextView
    private lateinit var loginBtn: Button

    private lateinit var auth: FirebaseAuth

    private lateinit var registerLink: TextView

    private lateinit var progressBar: ProgressBar

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        email = findViewById(R.id.logEmail)
        password = findViewById(R.id.logPassword)
        forgotPassword = findViewById(R.id.forgotPass)
        loginBtn = findViewById(R.id.loginBtn)

        auth = FirebaseAuth.getInstance()

        registerLink = findViewById(R.id.registerLink)

        progressBar = findViewById(R.id.login_progress_bar)
        progressBar.visibility = View.INVISIBLE

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        forgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            finish()
        }

        loginBtn.setOnClickListener {
            loginBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE

            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if(TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passText)) {
                email.error = "Заповніть, будь ласка, всі поля"
                password.error = "Заповніть, будь ласка, всі поля"
                loginBtn.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            } else {
                if(passText.isNotEmpty() || emailText.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(emailText, passText).addOnCompleteListener { task ->
                        loginBtn.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE

                        if(task.isSuccessful) {
                            Toast.makeText(baseContext, "Вхід успішний.", Toast.LENGTH_SHORT).show()
                            updateUI()
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Сталася помилка. " + task.exception!!.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    email.error = "Поле E-Mail не може бути пустим."
                    password.error = "Поле паролю не може бути пустим."
                    progressBar.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun updateUI() {
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }
}
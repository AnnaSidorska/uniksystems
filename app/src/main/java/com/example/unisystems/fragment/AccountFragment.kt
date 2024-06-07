package com.example.unisystems.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.unisystems.R
import com.example.unisystems.activity.MainActivity
import com.example.unisystems.activity.RegisterVisitActivity
import com.example.unisystems.activity.StartActivity
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


class AccountFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var logoutBtn: Button
    private lateinit var regFaceBtn: Button
    private lateinit var regVisitBtn: Button

    private lateinit var userImage: ImageView
    private lateinit var userTag: TextView
    private lateinit var userSurname: TextView
    private lateinit var userName: TextView
    private lateinit var userPatronym: TextView
    private lateinit var userSerialNumber: TextView
    private lateinit var userEmail: TextView
    private lateinit var expDate: TextView
    private lateinit var issueDate: TextView
    private lateinit var groupName: TextView
    private lateinit var position: TextView
    private lateinit var faculty: TextView
    private lateinit var educationForm: TextView

    private lateinit var linLayPosition: LinearLayout
    private lateinit var posTV: TextView
    private lateinit var efTV: TextView
    private lateinit var linLeyEduForm: LinearLayout
    private lateinit var groupTV: TextView
    private lateinit var linLayGroup: LinearLayout

    private lateinit var userRef: DatabaseReference

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        firebaseDatabase = FirebaseDatabase.getInstance()

        progressBar = view.findViewById(R.id.progressBar)
        logoutBtn = view.findViewById(R.id.logoutBtn)
        regFaceBtn = view.findViewById(R.id.regFaceBtn)
        regVisitBtn =view.findViewById(R.id.regVisitBtn)

        userImage = view.findViewById(R.id.userImage)

        userTag = view.findViewById(R.id.userTag)
        userSurname = view.findViewById(R.id.userSurname)
        userName = view.findViewById(R.id.userName)
        userPatronym = view.findViewById(R.id.userPatronym)
        userEmail = view.findViewById(R.id.userEmail)
        userSerialNumber = view.findViewById(R.id.serialNumber)
        expDate = view.findViewById(R.id.expDate)
        issueDate = view.findViewById(R.id.issueDate)
        groupName = view.findViewById(R.id.groupName)
        faculty = view.findViewById(R.id.facultyName)
        position = view.findViewById(R.id.position)
        educationForm = view.findViewById(R.id.eduForm)

        efTV = view.findViewById(R.id.efTV)
        linLeyEduForm = view.findViewById(R.id.linlayefTV)
        linLayPosition = view.findViewById(R.id.linlaypos)
        posTV = view.findViewById(R.id.posTV)
        groupTV = view.findViewById(R.id.groupTV)
        linLayGroup = view.findViewById(R.id.linlaygroup)

        loadUserImage()

        fetchUserData()

        logoutBtn.setOnClickListener {
            signOut()
        }

        regFaceBtn.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            //requireActivity().finish()
        }

        regVisitBtn.setOnClickListener {
            val intent = Intent(requireContext(), RegisterVisitActivity::class.java)
            startActivity(intent)
            //requireActivity().finish()
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser!!.uid
        Log.d(TAG, "User ID $userId")

        val database = Firebase.database(DATABASE_URL)
        userRef = database.getReference("users/$userId")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userType = dataSnapshot.child("userTag").getValue(String::class.java)

                    if (userType != null) {
                        when (userType) {
                            "student" -> setupStudentData(dataSnapshot)
                            "teacher" -> setupTeacherData(dataSnapshot)
                            "admin" -> setupAdminData(dataSnapshot)
                        }
                    }
                } else {
                    Log.d(TAG, "Користувача з ID $userId не існує")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Помилка при завантаженні даних користувача: ${databaseError.message}")
            }
        })
    }

    private fun setupStudentData(dataSnapshot: DataSnapshot) {
        val user = dataSnapshot.getValue(User::class.java)
        if (user != null) {
            userTag.text = "Студент"
            groupName.visibility = View.VISIBLE
            educationForm.visibility = View.VISIBLE
            position.visibility = View.GONE
            linLayPosition.visibility = View.GONE
            posTV.visibility = View.GONE

            userSurname.text = user.surname
            userName.text = user.name
            userPatronym.text = user.patronym
            userSerialNumber.text = user.serialNumber
            expDate.text = user.expDate
            issueDate.text = user.issueDate
            groupName.text = user.groupName
            faculty.text = user.faculty
            educationForm.text = user.educationForm

            userEmail.text = auth.currentUser!!.email
        }
    }

    private fun setupTeacherData(dataSnapshot: DataSnapshot) {
        val user = dataSnapshot.getValue(User::class.java)
        if (user != null) {
            userTag.text = "Викладач"
            groupName.visibility = View.GONE
            educationForm.visibility = View.GONE
            efTV.visibility = View.GONE
            linLeyEduForm.visibility = View.GONE
            position.visibility = View.VISIBLE
            linLayPosition.visibility = View.VISIBLE
            posTV.visibility = View.VISIBLE
            groupTV.visibility = View.GONE
            linLayGroup.visibility = View.GONE

            userSurname.text = user.surname
            userName.text = user.name
            userPatronym.text = user.patronym
            userSerialNumber.text = user.serialNumber
            expDate.text = user.expDate
            issueDate.text = user.issueDate
            position.text = user.position
            faculty.text = user.faculty

            userEmail.text = auth.currentUser!!.email
        }
    }

    private fun setupAdminData(dataSnapshot: DataSnapshot) {
        userTag.text = "Адміністратор"
        groupName.visibility = View.VISIBLE
        educationForm.visibility = View.VISIBLE
        position.visibility = View.VISIBLE

        userSurname.text = dataSnapshot.child("surname").getValue(String::class.java)
        userName.text = dataSnapshot.child("name").getValue(String::class.java)
        userPatronym.text = dataSnapshot.child("patronym").getValue(String::class.java)
        userSerialNumber.text = dataSnapshot.child("serialNumber").getValue(String::class.java)
        expDate.text = dataSnapshot.child("expDate").getValue(String::class.java)
        issueDate.text = dataSnapshot.child("issueDate").getValue(String::class.java)
        faculty.text = dataSnapshot.child("faculty").getValue(String::class.java)

        userEmail.text = auth.currentUser!!.email
    }

    private fun loadUserImage() {
        progressBar.visibility = View.VISIBLE
        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            progressBar.visibility = View.VISIBLE
            Glide.with(this@AccountFragment)
                .asBitmap()
                .load(photoUrl)
                .apply(
                    RequestOptions()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                )
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "Завантаження зображення не вдалося", e)
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        Log.d(TAG, "Зображення успішно завантажено")
                        return false
                    }
                })
                .into(userImage)
        } else {
            Log.e(TAG, "URL зображення користувача відсутній")
            progressBar.visibility = View.GONE
        }
    }


    private fun signOut() {
        auth.signOut()
        Toast.makeText(requireContext(), "Ви покинули ваш обліковий запис", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), StartActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        const val TAG = "AccountFragment"
        const val DATABASE_URL = "https://uni-k-systems-default-rtdb.europe-west1.firebasedatabase.app"
    }
}

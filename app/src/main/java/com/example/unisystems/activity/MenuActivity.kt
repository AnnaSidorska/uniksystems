package com.example.unisystems.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.unisystems.R
import com.example.unisystems.databinding.ActivityMenuBinding
import com.example.unisystems.fragment.AccountFragment
import com.example.unisystems.fragment.AdminFragment
import com.example.unisystems.fragment.CheckInFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MenuActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var binding: ActivityMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        val uid = user.uid
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(AccountFragment())

        if (uid == ADMIN_UID)
            R.id.adminpanel else {
            binding.bottomNavigationView.menu.removeItem(R.id.adminpanel)
        }

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile -> replaceFragment(AccountFragment())
                R.id.checkin -> replaceFragment(CheckInFragment())
                R.id.adminpanel -> {
                    if (uid == ADMIN_UID) {
                        replaceFragment(AdminFragment())
                    } else {
                        Log.w("MenuActivity", "У доступі відмовлено: Користувач не має прав для доступу до адмін-панелі")
                    }
                }
                else -> {
                    Log.w("MenuActivity", "Вибрано невідомий пункт меню: ${menuItem.itemId}")
                }
            }
            true
        }


    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }

    companion object {
        const val ADMIN_UID = "U2Dwz6GyZoUnzsIIf23HG1fk2d73"
    }
}
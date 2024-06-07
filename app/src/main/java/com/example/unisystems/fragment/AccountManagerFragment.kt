package com.example.unisystems.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unisystems.R
import com.example.unisystems.adapter.AccountRVAdapter
import com.example.unisystems.model.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class AccountManagerFragment : Fragment(), AccountRVAdapter.ItemClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var accountList: MutableList<User>
    private lateinit var adapter: AccountRVAdapter
    private lateinit var linearProgressBar: ProgressBar
    private lateinit var firebaseDatabase: FirebaseDatabase
    private val databaseUrl: String = "https://uni-k-systems-default-rtdb.europe-west1.firebasedatabase.app"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_manager, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.check_in)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseDatabase = FirebaseDatabase.getInstance()

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.isVerticalScrollBarEnabled = true

        accountList = mutableListOf()
        adapter = AccountRVAdapter(accountList, this)

        linearProgressBar = view.findViewById(R.id.linearProgressBar)
        linearProgressBar.visibility = View.INVISIBLE

        initAccountRV()
    }

    private fun initAccountRV() {
        val database = Firebase.database(databaseUrl)
        val databaseReference = database.getReference("users")
        linearProgressBar.visibility = View.VISIBLE
        adapter = AccountRVAdapter(accountList, this)

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                accountList.clear()

                for (childSnapshot in dataSnapshot.children) {
                    val user = childSnapshot.getValue(User::class.java)
                    if (user != null) {
                        accountList.add(user)
                    }
                }

                adapter.notifyDataSetChanged()
                recyclerView.adapter = adapter
                linearProgressBar.visibility = View.INVISIBLE
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Failed to read value.", databaseError.toException())
                linearProgressBar.visibility = View.INVISIBLE
            }
        })

        recyclerView.adapter = adapter
    }

    private fun showConfirmationDialog(context: Context, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Підтвердіть дію")
            .setMessage(message)
            .setPositiveButton("Підтвердити") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDeleteClick(position: Int) {
        val user = accountList[position]
        val userID = user.uid
        val context = requireContext()

        val tag: String = when (user.userTag) {
            "teacher" -> "Викладач"
            "student" -> "Студент"
            else -> null.toString()
        }

        showConfirmationDialog(context, "Ви впевнені, що хочете видалити цей обліковий запис?\n" +
                "$tag ${user.surname} ${user.name} ${user.patronym}") {
            val database = Firebase.database(databaseUrl)
            val databaseRef = database.getReference("users/$userID")
            databaseRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseAuth.getInstance().currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            Log.d(TAG, "Акаунт успішно видалено")
                            accountList.removeAt(position)
                            adapter.notifyItemRemoved(position)
                        } else {
                            Log.e(TAG, "Помилка видалення акаунту: ${deleteTask.exception}")
                        }
                    }
                } else {
                    Log.e(TAG, "Помилка видалення даних користувача: ${task.exception}")
                }
            }
        }
    }




    companion object {
        const val TAG = "AccountManagerFragment"
    }
}
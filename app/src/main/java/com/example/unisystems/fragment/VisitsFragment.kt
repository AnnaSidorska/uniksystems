package com.example.unisystems.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.unisystems.R
import com.example.unisystems.adapter.FacesRVAdapter
import com.example.unisystems.model.Face
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore

class VisitsFragment : Fragment(), FacesRVAdapter.ItemClickListener {
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var facesList: MutableList<Face>
    private lateinit var adapter: FacesRVAdapter
    private lateinit var linearProgressBar: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_visits, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.visits_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.isVerticalScrollBarEnabled = true

        facesList = mutableListOf()
        adapter = FacesRVAdapter(facesList, this)

        linearProgressBar = view.findViewById(R.id.linearProgressBar)
        linearProgressBar.visibility = View.INVISIBLE

        firebaseDatabase = FirebaseDatabase.getInstance()

        initVisitsRV()
    }

    private fun initVisitsRV() {
        val db = Firebase.firestore

        linearProgressBar.visibility = View.VISIBLE

        adapter = FacesRVAdapter(facesList, this)


        db.collection("registerFaces")
            .get()
            .addOnSuccessListener { result ->
                facesList.clear()

                for (document in result) {
                    val id = document.id
                    val timestamp = document.getLong("registrationTime")
                    val name = document.getString("name") ?: ""
                    val surname = document.getString("surname") ?: ""
                    val place = document.getString("building") ?: ""
                    val photoUrl = document.getString("imageUri") ?: ""

                    val face = Face(id, photoUrl, timestamp!!, name, surname, place)

                    facesList.add(face)
                }
                facesList.sortWith(compareBy { it.timestamp as? Comparable<*> })
                facesList.reverse()

                @Suppress("NotifyDataSetChanged")
                adapter.notifyDataSetChanged()
                recyclerView.adapter = adapter
                linearProgressBar.visibility = View.INVISIBLE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Неможливо завантажити відвідування", Toast.LENGTH_SHORT).show()
                linearProgressBar.visibility = View.INVISIBLE
            }
    }

    @Suppress("SameParameterValue")
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
        val context = requireContext()
        val message = "Ви впевнені, що хочете видалити це відвідування?"
        showConfirmationDialog(context, message) {
            val db = Firebase.firestore
            val faceID = facesList[position].id
            db.collection("registerFaces")
                .document(faceID)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Обличчя видалено успішно", Toast.LENGTH_SHORT).show()
                    facesList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Помилка видалення обличчя: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    companion object {
        const val TAG = "Visits Fragment"
    }

}
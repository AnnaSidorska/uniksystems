package com.example.unisystems.adapter

import android.text.format.DateFormat
import java.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.unisystems.R
import com.example.unisystems.model.Face
import com.squareup.picasso.Picasso
import java.util.Locale

class FacesRVAdapter(
    private var facesList: MutableList<Face>,
    private val clickListener: ItemClickListener
) : RecyclerView.Adapter<FacesRVAdapter.SaveViewHolder>() {

    inner class SaveViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val faceIV: ImageView = itemView.findViewById(R.id.faceImageView)
        val timeTV: TextView = itemView.findViewById(R.id.timeTextView)
        val nameTV: TextView = itemView.findViewById(R.id.nameTextView)
        val surnameTV: TextView = itemView.findViewById(R.id.surnameTextView)
        val placeTV: TextView = itemView.findViewById(R.id.placeTextView)
        val deleteIV: ImageView = itemView.findViewById(R.id.deleteVisit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaveViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.faces_rv_item, parent, false)
        return this.SaveViewHolder(view)
    }

    override fun getItemCount(): Int {
        return facesList.size
    }

    override fun onBindViewHolder(holder: SaveViewHolder, position: Int) {
        val faces: Face = facesList[position]

        Picasso.get().load(faces.faceUrl).into(holder.faceIV)
        holder.timeTV.text = timeStampToString(faces.timestamp)
        holder.nameTV.text = faces.name
        holder.surnameTV.text = faces.surname
        holder.placeTV.text = faces.place

        holder.deleteIV.setOnClickListener {
            clickListener.onDeleteClick(position)
        }

    }

    private fun timeStampToString(timestamp: Any): String {
        val calendar: Calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp as Long
        return DateFormat.format("dd.MM.yyyy HH:mm", calendar).toString()
    }

    interface ItemClickListener {
        fun onDeleteClick(position: Int)
    }

}
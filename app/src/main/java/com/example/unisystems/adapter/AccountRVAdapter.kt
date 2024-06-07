package com.example.unisystems.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.unisystems.R
import com.example.unisystems.model.User
import com.google.firebase.auth.FirebaseAuth

class AccountRVAdapter(
    private val accountList: MutableList<User>,
    private val clickListener: ItemClickListener
): RecyclerView.Adapter<AccountRVAdapter.SaveViewHolder>() {

    inner class SaveViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val imageIV: ImageView = itemView.findViewById(R.id.userImageView)
        val tagTV: TextView = itemView.findViewById(R.id.userTagTV)
        val surnameTV: TextView = itemView.findViewById(R.id.userSurnameTV)
        val nameTV: TextView = itemView.findViewById(R.id.userNameTV)
        val patronymTV: TextView = itemView.findViewById(R.id.userPatronymTV)
        val serialNumberTV: TextView = itemView.findViewById(R.id.userNumberTV)
        val educationFormTV: TextView = itemView.findViewById(R.id.userEduFormTV)
        val facultyTV: TextView = itemView.findViewById(R.id.userFacultyTV)
        val expDateTV: TextView = itemView.findViewById(R.id.userExpDateTV)
        val issueDateTV: TextView = itemView.findViewById(R.id.userIssueDateTV)
        val groupTV: TextView = itemView.findViewById(R.id.userGroupTV)
        val positionTV: TextView = itemView.findViewById(R.id.userPositionTV)
        val deleteBtn: ImageView = itemView.findViewById(R.id.deleteAccount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountRVAdapter.SaveViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.account_rv_item, parent, false)
        return this.SaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountRVAdapter.SaveViewHolder, position: Int) {
        val account: User = accountList[position]

        when (account.userTag) {
            "student" -> {
                holder.tagTV.text = "Студент"
                holder.educationFormTV.visibility = View.VISIBLE
                holder.groupTV.visibility = View.VISIBLE
                holder.educationFormTV.text = account.educationForm
                holder.groupTV.text = account.groupName
                holder.positionTV.visibility = View.GONE
            }
            "teacher" -> {
                holder.tagTV.text = "Викладач"
                holder.educationFormTV.visibility = View.GONE
                holder.groupTV.visibility = View.GONE
                holder.positionTV.visibility = View.VISIBLE
                holder.positionTV.text = account.position
            }
            "admin" -> {
                holder.tagTV.text = "Адміністратор"
                holder.educationFormTV.visibility = View.GONE
                holder.groupTV.visibility = View.GONE
                holder.positionTV.visibility = View.GONE
            }
        }

        val photoUrl = account.photoUrl

        holder.surnameTV.text = account.surname
        holder.nameTV.text = account.name
        holder.patronymTV.text = account.patronym
        holder.serialNumberTV.text = account.serialNumber
        holder.facultyTV.text = account.faculty
        holder.issueDateTV.text = account.issueDate
        holder.expDateTV.text = account.expDate

        val isUserAdmin = account.uid == FirebaseAuth.getInstance().currentUser?.uid
        holder.deleteBtn.visibility = if(isUserAdmin) View.GONE else View.VISIBLE

        holder.deleteBtn.setOnClickListener {
            clickListener.onDeleteClick(position)
        }

        Glide.with(holder.itemView)
            .load(photoUrl)
            .placeholder(R.drawable.boy)
            .into(holder.imageIV)
    }


    interface ItemClickListener {
        fun onDeleteClick(position: Int)
    }

    override fun getItemCount(): Int {
        return accountList.size
    }
}

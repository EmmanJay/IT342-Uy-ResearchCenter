package com.example.researchcenter.features.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R

data class InviteCandidate(
    val id: Long,
    val email: String,
    val name: String
)

class UserSearchAdapter(
    private val items: List<InviteCandidate>,
    private val onInvite: (InviteCandidate) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvInviteName)
        val tvEmail: TextView = view.findViewById(R.id.tvInviteEmail)
        val btnInvite: Button = view.findViewById(R.id.btnInviteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_invite_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name.ifBlank { item.email }
        holder.tvEmail.text = item.email
        holder.btnInvite.setOnClickListener { onInvite(item) }
    }

    override fun getItemCount() = items.size
}
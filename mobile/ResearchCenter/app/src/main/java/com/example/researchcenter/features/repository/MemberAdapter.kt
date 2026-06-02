package com.example.researchcenter.features.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.RepositoryMember
import com.example.researchcenter.shared.ui.UserAvatarView

class MemberAdapter(
    private val items: List<RepositoryMember>,
    private val isOwner: Boolean,
    private val onRemove: (RepositoryMember) -> Unit
) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: UserAvatarView = view.findViewById(R.id.avatar_view)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_email)
        val tvRole: TextView = view.findViewById(R.id.tv_role)
        val btnRemove: Button = view.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = items[position]
        holder.tvName.text = member.name
        holder.tvEmail.text = member.email
        holder.tvRole.text = member.role
        holder.avatarView.setUser(member.name, member.email, member.profilePicture)

        val canRemove = isOwner && member.role != "OWNER"
        holder.btnRemove.visibility = if (canRemove) View.VISIBLE else View.GONE
        
        holder.btnRemove.setOnClickListener { onRemove(member) }
    }

    override fun getItemCount() = items.size
}
